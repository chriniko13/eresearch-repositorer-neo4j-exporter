package com.chriniko.eresearchreponeo4jexporter.repository.mongo;

import com.chriniko.eresearchreponeo4jexporter.domain.mongo.Author;
import com.chriniko.eresearchreponeo4jexporter.domain.mongo.Record;
import com.chriniko.eresearchreponeo4jexporter.domain.mongo.RetrievedRecordDto;
import com.chriniko.eresearchreponeo4jexporter.error.ProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Repository
public class RecordRepository implements EresearchRepositorerRepository<Record, RetrievedRecordDto, Author> {

    private static final String FILENAME_PREFIX = "RECORD";

    private static final String RECORD_TYPE = "RECORD";

    private static final boolean APPLY_DISTINCTION_ON_SEARCH_RESULTS = true;

    private static final String CONTENT_TYPE = "application/octet-stream";

    private static final String NO_VALUE = "NoValue";

    private static final String REGEX_FOR_ALL = ".*";

    private final Pattern findAllRecordsPattern;

    private final GridFsTemplate gridFsTemplate;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final GridFSBucket gridFSBucket;

    @Autowired
    public RecordRepository(GridFsTemplate gridFsTemplate,
                            Clock clock,
                            ObjectMapper objectMapper,
                            GridFSBucket gridFSBucket) {
        this.gridFsTemplate = gridFsTemplate;
        this.clock = clock;
        this.objectMapper = objectMapper;

        findAllRecordsPattern = Pattern.compile(REGEX_FOR_ALL);
        this.gridFSBucket = gridFSBucket;
    }

    private List<GridFSFile> extractFiles(GridFSFindIterable iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
    }

    @Override
    public boolean deleteAll() {

        GridFSFindIterable iterable = gridFsTemplate.find(Query.query(Criteria.where("filename").regex(FILENAME_PREFIX + findAllRecordsPattern)));
        List<GridFSFile> gridFSDBFiles = extractFiles(iterable);

        if (gridFSDBFiles.size() == 0) {
            throw new ProcessingException("no records to delete");
        }

        gridFsTemplate.delete(Query.query(Criteria.where("filename").regex(FILENAME_PREFIX + findAllRecordsPattern)));

        return findAll(false).size() == 0;
    }

    @Override
    public boolean delete(String filename) {
        GridFSFindIterable iterable = gridFsTemplate.find(Query.query(Criteria.where("filename").is(filename)));
        List<GridFSFile> gridFSDBFiles = extractFiles(iterable);

        if (gridFSDBFiles.size() > 1) {
            throw new ProcessingException("no unique results exist based on provided filename");
        }

        if (gridFSDBFiles.size() == 0) {
            throw new ProcessingException("no record exists based on provided filename");
        }

        gridFsTemplate.delete(Query.query(Criteria.where("filename").is(filename)));

        gridFSDBFiles = extractFiles(gridFsTemplate.find(Query.query(Criteria.where("filename").is(filename))));
        return gridFSDBFiles.size() == 0;
    }

    @Override
    public Collection<RetrievedRecordDto> find(String filename, boolean fullFetch) {

        final Collection<RetrievedRecordDto> retrievedRecordDtos = new ArrayList<>();

        List<GridFSFile> gridFSDBFiles = extractFiles(gridFsTemplate.find(Query.query(Criteria.where("filename").is(filename))));

        populateRetrievedRecordsWithResults(retrievedRecordDtos, gridFSDBFiles, fullFetch);

        return retrievedRecordDtos;
    }

    @Override
    public Collection<RetrievedRecordDto> find(boolean fullFetch, Author mainAuthorName) {

        //------------- do main search --------------
        final Collection<RetrievedRecordDto> retrievedRecordDtos = APPLY_DISTINCTION_ON_SEARCH_RESULTS ? new HashSet<>() : new ArrayList<>();

        String filenameToSearchFor = createFilenameToSearchFor(mainAuthorName);

        final List<GridFSFile> retrievedGridFSDBFilesFromMainSearch
                = extractFiles(gridFsTemplate.find(Query.query(Criteria.where("filename").regex(filenameToSearchFor))));

        populateRetrievedRecordsWithResults(retrievedRecordDtos, retrievedGridFSDBFilesFromMainSearch, fullFetch);

        //--------- and then do additional search based on splitted metadata info ------------
        List<GridFSFile> retrievedGridFSDBFilesFromNameVariantsSearch = extractFiles(gridFsTemplate.find(
                Query.query(
                        Criteria.where("metadata.authorRecordName.firstname")
                                .regex(REGEX_FOR_ALL + mainAuthorName.getFirstname() + REGEX_FOR_ALL)
                                .and("metadata.authorRecordName.initials")
                                .regex(REGEX_FOR_ALL + mainAuthorName.getInitials() + REGEX_FOR_ALL)
                                .and("metadata.authorRecordName.lastname")
                                .regex(REGEX_FOR_ALL + mainAuthorName.getSurname() + REGEX_FOR_ALL)
                                .and("metadata.recordType")
                                .regex(RECORD_TYPE)
                )
        ));

        populateRetrievedRecordsWithResults(retrievedRecordDtos, retrievedGridFSDBFilesFromNameVariantsSearch, fullFetch);

        return retrievedRecordDtos;
    }

    @Override
    public Collection<RetrievedRecordDto> findAll(boolean fullFetch) {
        final List<RetrievedRecordDto> retrievedRecordDtos = new ArrayList<>();

        List<GridFSFile> gridFSDBFiles = extractFiles(gridFsTemplate.find(Query.query(Criteria.where("filename").regex(FILENAME_PREFIX + findAllRecordsPattern))));

        populateRetrievedRecordsWithResults(retrievedRecordDtos, gridFSDBFiles, fullFetch);

        return retrievedRecordDtos;
    }

    @Override
    public void store(Record record) {

        try {
            //first serialize record...
            byte[] bytes = objectMapper.writeValueAsBytes(record);

            //then transform the serialized record into an input stream...
            try (InputStream is = new ByteArrayInputStream(bytes)) {

                //finally store the info...
                String fileName = createFilename(record) + "#" + getStringifiedDate(record);
                DBObject metaData = createMetadata(record);
                gridFsTemplate.store(is, fileName, CONTENT_TYPE, metaData);
            }
        } catch (Exception error) {
            log.error("could not store record", error);
            throw new ProcessingException("could not store record");
        }
    }

    private String createFilename(Record record) {
        return FILENAME_PREFIX
                + normalizeInfo(record.getFirstname())
                + "_"
                + normalizeInfo(record.getInitials())
                + "_"
                + normalizeInfo(record.getLastname());
    }

    private DBObject createMetadata(Record record) {
        DBObject metaData = new BasicDBObject();

        DBObject authorRecordName = new BasicDBObject();
        authorRecordName.put("firstname", record.getFirstname());
        authorRecordName.put("initials", record.getInitials());
        authorRecordName.put("lastname", record.getLastname());

        metaData.put("authorRecordName", authorRecordName);
        metaData.put("recordCreatedAt", getStringifiedDate(record));
        metaData.put("recordType", RECORD_TYPE);

        return metaData;
    }

    private String getStringifiedDate(Record record) {
        return LocalDateTime.ofInstant(record.getCreatedAt(), clock.getZone()).toString();
    }

    private String normalizeInfo(String info) {
        return Optional.ofNullable(info).filter(i -> !i.isEmpty()).orElse(NO_VALUE);
    }

    private String createFilenameToSearchFor(Author author) {
        return FILENAME_PREFIX
                + normalizeInfo(author.getFirstname()) + REGEX_FOR_ALL
                + "_"
                + normalizeInfo(author.getInitials()) + REGEX_FOR_ALL
                + "_"
                + normalizeInfo(author.getSurname()) + REGEX_FOR_ALL;
    }

    private void populateRetrievedRecordsWithResults(Collection<RetrievedRecordDto> retrievedRecordDtos,
                                                     List<GridFSFile> gridFSDBFiles,
                                                     boolean fullFetch) {

        for (GridFSFile gridFSDBFile : gridFSDBFiles) {
            String filename = gridFSDBFile.getFilename();

            RetrievedRecordDto retrievedRecordDto = new RetrievedRecordDto();
            retrievedRecordDto.setFilename(filename);

            if (fullFetch) {
                GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gridFSDBFile.getObjectId());
                GridFsResource gridFsResource = new GridFsResource(gridFSDBFile, gridFSDownloadStream);

                try {
                    Record record = deserializeStoredRecord(gridFsResource.getInputStream());
                    retrievedRecordDto.setRecord(record);
                } catch (IOException error) {
                    throw new ProcessingException("could not obtain grid fs resource input stream");
                }
            }
            retrievedRecordDtos.add(retrievedRecordDto);
        }
    }

    private Record deserializeStoredRecord(InputStream inputStream) {
        try {
            byte[] bytes = ByteStreams.toByteArray(inputStream);
            return objectMapper.readValue(bytes, Record.class);
        } catch (Exception error) {
            log.error("could not deserialize stored record", error);
            throw new ProcessingException("could not deserialize stored record");
        }
    }
}
