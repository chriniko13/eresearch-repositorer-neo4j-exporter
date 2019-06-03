### ERESEARCH REPOSITORER NEO4J EXPORTER

#### Description
UNDER CONSTRUCTION


#### Dependencies

* Neo4J Docker Run:
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


#### Sample Queries

1.
```
MATCH (a:Author{fullname:"Christos Skourlas"}) RETURN a;
```

2.
```
MATCH (a:Author{fullname:"Christos Skourlas"}), (b:Author{fullname:"Anastasios Tsolakidis"}),
p = shortestPath((a)-[*..15]-(b))
return p;
```

3.
```
MATCH (a:Author{fullname:"Christos Skourlas"}), (b:Author{fullname:"Anastasios Tsolakidis"}),
p = shortestPath((a)-[*..15]-(b))
return p;
```

4.
```
MATCH (a:Author{fullname:"Christos Skourlas"}), (b:Author{fullname:"Anastasios Tsolakidis"}),
p = allShortestPaths((a)-[*..15]-(b))
return p;
```

5.
```
MATCH (a:Author{fullname:"Katerina Georgouli"}), (b:Author{fullname:"Grammati Pantziou"}),
p = shortestPath((a)-[*..15]-(b))
return p;
```

6.
```
match (e:Entry)
return  (e.title);
```

7.
```
match (e:Entry)
return  distinct (e.title);
```

8.
```
match (e:Entry)
return  count (e.title);
```

9.
```
match (a:Author)
return  (a.fullname);
```
