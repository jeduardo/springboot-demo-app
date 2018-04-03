# Springboot demo app

This is a small demo app created in Spring boot created to exercise automated deployments
of non-trivial applications. It is a Java version of the [flask-demo-app](https://github.com/jeduardo/flask-demo-app).

* Configuration based on environment variables
* Database-backed entries
* JSON API
* Stateless
* Support for X-Request-ID header for request tracking
* Logging with JSON to stdout


## Model

The only model defined for the application is the model of an `Entry`.

An `Entry` is composed by the following fields:

* id: unique identifier for an entry. This field is generated automatically
and	cannot be modified.
* description: a description for the `Entry`.
* comment: a comment over the `Entry`.

## Configuration

The following environment variables can be configured:

* APP_DATABASE_URL (mandatory): points to the backing database

* APP_DATABASE_USER (mandatory): holds the database user

* APP_DATABASE_PASSWORD (mandatory): holds the database password

* APP_DATABASE_SCHEMA (mandatory): specify what the persistence layer should do with the schema during the boot according to the options:
    * none: nothing is done
    * validate: schema is validated during initialization time
    * create: schema is reset during initialization time
    * update: schema is updated during initialization time

* APP_LOG_LEVEL (optional): allows configuring the application log level. If it is
not specified, the default log level will be INFO.

* APP_FLYWAY_ENABLED (optional): whether Flyway-based migrations should be executed during boot. If left out, it will be enabled by default.

## Database migration

Before running the application, the database must be setup. There are built-in migration
scripts with Flyway to accomplish this. In order to run this, the following maven command
must be executed:

```ShellSession
$ export APP_DATABASE_USER=pxdtwhsbazfwdr
$ export APP_DATABASE_PASSWORD=fc06ba6b5f75e0c1ab303eb26a1332315ee53c0314b9ff34dcbbe7cc0b21005f
$ export APP_DATABASE_URL='jdbc:postgresql://ec2-54-247-189-64.eu-west-1.compute.amazonaws.com:5432/da8s86ph6ufuq7?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory'
$ mvn flyway:migrate
```

## Deployment

All requirements for the application are specified in the `pom.xml` file.

The application is build with the command `mvn package`. The result will be an executable JAR under target/entries-X.X.X.jar, where X.X.X is the version number.

To execute the application, define all the required configuration variables and run the application with `java -jar target/entries-X.X.X.jar`, as below:

```ShellSession
$ export APP_DATABASE_USER=pxdtwhsbazfwdr
$ export APP_DATABASE_PASSWORD=fc06ba6b5f75e0c1ab303eb26a1332315ee53c0314b9ff34dcbbe7cc0b21005f
$ export APP_DATABASE_URL='jdbc:postgresql://ec2-54-247-189-64.eu-west-1.compute.amazonaws.com:5432/da8s86ph6ufuq7?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory'
$ java -jar target/entries-X.X.X.jar
```

On the first execution, create the schema by also exporting `APP_DATABASE_SCHEMA=create`.


## API

The following endpoints are offered:

* /api/v1/entries
	* parameters: none
	* methods: GET
	* description: list all entries in the database
	* accepts: all formats
	* returns: application/json

* /api/v1/entries
	* parameters: JSON object representing an `Entry`
	* methods: POST
	* description: create a new entry
	* accepts: application/json
	* returns: application/json

* /api/v1/entries/<id>
	* parameters: JSON object representing an `Entry`
	* methods: POST
	* description: update an existing `Entry`
	* accepts: application/json
	* returns: application/json

* /api/v1/entries/<id>
	* parameters: entry id
	* methods: DELETE
	* description: remove an entry from the database
	* accepts: all formats
	* returns: application/json

* /api/v1/status
	* parameters: none
	* methods: GET
	* description: return "OK" if the application is reachable
	* accepts: all formats
	* returns: application/json

# License

This code is licensed under the MIT License.