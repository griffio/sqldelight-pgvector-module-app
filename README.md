# SqlDelight 2.1.x Postgresql PgVector module support prototype 

https://github.com/cashapp/sqldelight

**Experimental**

Use with sqldelight branch https://github.com/griffio/sqldelight/tree/postgresql-modules with minor changes (Pushed to local maven `publishToMavenLocal`)

---

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
            module(project(":pgvector-module")) // module can be local project or external dependency
        }
    }
}
```

```sql

CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    embedding VECTOR(3)
);

CREATE INDEX idx_embedding_hnsw ON items USING hnsw (embedding vector_l2_ops);

CREATE INDEX idx_embedding_ivfflat ON items USING ivfflat (embedding vector_l2_ops) WITH (lists = 100);

insert:
INSERT INTO items (embedding) VALUES ('[1,2,3]'), ('[4,5,6]');

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

**TODO**

Query Operators https://github.com/pgvector/pgvector/tree/master?tab=readme-ov-file#querying

There are problems extending an existing grammar through more than one level of inheritance. This would require fixes to
https://github.com/sqldelight/Grammar-Kit-Composer - Such that, when adding to an existing type (e.g. data type) concatenation of PostgreSql types is required

SqlDelight needs this fix https://github.com/sqldelight/sqldelight/pull/5625 for the module resolver to be the first

PostgreSqlTypeResolver needs to be (open) inheritable rather than use delegation e.g. override `definitionType` expects to be called

```shell
createdb vector && 
./gradlew build &&
./gradlew flywayMigrate
```
