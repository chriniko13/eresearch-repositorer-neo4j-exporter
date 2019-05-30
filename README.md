### ERESEARCH REPOSITORER NEO4J EXPORTER

#### Description
UNDER CONSTRUCTION


#### Dependencies

* Run:
    ```
    docker run \
        --name testneo4j \
        -p7474:7474 -p7687:7687 \
        -d \
        -v $HOME/neo4j/data:/data \
        -v $HOME/neo4j/logs:/logs \
        -v $HOME/neo4j/import:/var/lib/neo4j/import \
        -v $HOME/neo4j/plugins:/plugins \
        --env NEO4J_AUTH=neo4j/test \
        neo4j:latest
    ```
    
* For more info see: `https://neo4j.com/developer/docker-run-neo4j/`
