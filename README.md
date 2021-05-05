Testing Support for Neo4j Integrations
======================================

Provides the ability to test against Neo4j in the presence of potential dependency version clashes. A classic example
 are different versions of Lucene when running Neo4j and Elasticsearch in the same JVM.

This project starts Neo4j using a different classloader, hence alleviating the issue.

For Neo4 2.3.0+, use

```
<dependency>
    <groupId>com.graphaware.integration.neo4j</groupId>
    <artifactId>neo4j-tests-integration</artifactId>
    <version>2.3.3.1</version>
    <scope>test</scope>
</dependency>
```

License
-------

Copyright (c) 2015-2016 GraphAware

GraphAware is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with this program.
If not, see <http://www.gnu.org/licenses/>.
