# SqlDelight 2.1.x Postgresql PgVector module support prototype 

https://github.com/cashapp/sqldelight

**Experimental**

Use with SqlDelight `2.1.0`

---

## Usage

Instead of a new dialect or adding PostgreSql extensions into the core PostgreSql grammar e.g. https://postgis.net/ and https://github.com/pgvector/pgvector

Use a custom SqlDelight module to implement grammar and type resolvers for PgVector operations

```kotlin
sqldelight {
    databases {
        create("Sample") {
            deriveSchemaFromMigrations.set(true)
            migrationOutputDirectory = file("$buildDir/generated/migrations")
            migrationOutputFileFormat = ".sql"
            packageName.set("griffio.queries")
            dialect(libs.sqldelight.postgresql.dialect)
            module(project(":pgvector-module")) // module can be local project
            // or external dependency module("io.github.griffio:sqldelight-pgvector:0.0.1")
        }
    }
}
```

`pgvector-module` published in Maven Central https://central.sonatype.com/artifact/io.github.griffio/sqldelight-pgvector/versions

`io.github.griffio:sqldelight-pgvector:0.0.1`

```sql

CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    embedding VECTOR(3),
    bits BIT(3)
);

CREATE INDEX idx_embedding_hnsw ON items USING hnsw (embedding vector_l2_ops);

CREATE INDEX idx_embedding_ivfflat ON items USING ivfflat (embedding vector_l2_ops) WITH (lists = 100);

insert:
INSERT INTO items (embedding, bits) VALUES ('[1,2,3]', '000'), ('[4,5,6], '111');

select:
SELECT *
FROM items;

selectEmbeddings:
SELECT * FROM items ORDER BY embedding <-> '[3,1,2]' LIMIT 5;

selectWithVector:
SELECT * FROM items ORDER BY embedding <-> ?::VECTOR LIMIT 5;

selectSubVector:
SELECT subvector(?::VECTOR, 1, 3);

selectCosineDistance:
SELECT cosine_distance('[1,1]'::VECTOR, '[-1,-1]');

selectBinaryQuantize:
SELECT binary_quantize('[0,0.1,-0.2,-0.3,0.4,0.5,0.6,-0.7,0.8,-0.9,1]'::VECTOR);
```

---

Movies Sample

Create LLM embeddings from the movie plot summaries and use prompt.

Uses cosine similarity to find movie plot summaries that are closest to a given prompt

```sql
-- Create the movie_plots table with an embedding column
CREATE TABLE movie_plots (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    year INTEGER,
    genre VARCHAR(100),
    plot_summary TEXT NOT NULL,
    embedding VECTOR(1024)  -- Adjust dimension based on your embedding model
);

-- An HNSW index creates a multilayer graph.
-- It has better query performance than IVFFlat (in terms of speed-recall tradeoff),
-- but has slower build times and uses more memory.
-- Also, an index can be created without any data in the table since there isn’t a training step like IVFFlat.
-- Add an index for each distance function you want to use.
CREATE INDEX idx_movie_embedding_hnsw ON movie_plots USING hnsw (embedding vector_cosine_ops);
```

```sql

select:
SELECT id, title, year, genre, plot_summary FROM movie_plots ORDER BY year;

update:
UPDATE movie_plots
SET embedding = jsonb_path_query_first(:embeddingResponse::JSONB, '$.data[*].embedding')::TEXT::VECTOR
WHERE id = :movieId;

similaritySearch:
WITH search(vec) AS (
  SELECT jsonb_path_query_first(:embeddingResponse::JSONB, '$.data[*].embedding')::TEXT::VECTOR
)
SELECT id, title, year, genre, 1 - (embedding <=> search.vec) AS similarityScore
FROM movie_plots, search
ORDER BY embedding <=> search.vec;


```

```
-------------------------------------
Movies about questioning what's real.
-------------------------------------
The Truman Show ( 1998 Sci-Fi Drama Comedy ) Score: 0.4663206020784745
The Matrix ( 1999 Sci-Fi Action ) Score: 0.46191100435653554
Eternal Sunshine of the Spotless Mind ( 2004 Romantic Sci-Fi Drama ) Score: 0.4469298720359802
Inception ( 2010 Sci-Fi Thriller ) Score: 0.40091077083881144
Interstellar ( 2014 Sci-Fi Drama ) Score: 0.35195842385292053
Up ( 2009 Animation Adventure ) Score: 0.3490072412799696
Arrival ( 2016 Sci-Fi Drama ) Score: 0.33152367692903795
Parasite ( 2019 Dark Comedy Thriller ) Score: 0.29843956232070923
The Shawshank Redemption ( 1994 Drama ) Score: 0.25976793526219977
Groundhog Day ( 1993 Comedy Fantasy ) Score: 0.25180159498443677
```

see https://github.com/pgvector/pgvector/blob/master/test/sql/vector_type.sql

**TODO**

Add more types - halfvec, sparse vectors 

Extending an existing grammar through more than one level of inheritance isn't supported by grammar generator -  
This would require fixes to https://github.com/sqldelight/Grammar-Kit-Composer - work around is to override manually e.g.

```kotlin
 PostgreSqlParserUtil.type_name = Parser { psiBuilder, i ->
            type_name?.parse(psiBuilder, i) ?: PgvectorParser.type_name_real(psiBuilder, i)
                    || PostgreSqlParser.type_name_real(psiBuilder, i)
        }
```

SqlDelight needs this fix https://github.com/sqldelight/sqldelight/pull/5677 for the modules to work as the
PostgreSqlTypeResolver needs to be (open) inheritable rather than use delegation e.g. override `definitionType` method
expects to be called via inheritance.

Use Jdbc types https://github.com/pgvector/pgvector-java either directly in type resolver or using SqlDelight type adapters

```shell
createdb vector && 
./gradlew build &&
./gradlew flywayMigrate
```
