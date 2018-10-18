# Limiter
_Data file format service_

Service to convert specific CSV and fixed-format files into HTML

### Deployment pipeline
[![Build Status](https://travis-ci.org/sothach/stratum.png)](https://travis-ci.org/sothach/limiter) >>
[![Coverage Status](https://coveralls.io/repos/github/sothach/limiter/badge.svg?branch=master)](https://coveralls.io/github/sothach/limiter?branch=master) >>
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a688282e09a04ddeb6d0b29f2c8b82e1)](https://www.codacy.com/project/sothach/limiter/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=sothach/stratum&amp;utm_campaign=Badge_Grade_Dashboard) >>
![Heroku](https://heroku-badge.herokuapp.com/?app=limiter-be&root=index.html)

[![Deploy](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy)

## Assumptions
* All fields are mandatory 
* Provide service via REST interface (POST)
* No UI, just return `<html>` block
* No persistence

## Approach
### Platform
The Play! framework provides a good starting point for this project: it's convention-over-configuration approach to
URL routes, configuration and HTTP actions simplify the work needed
### App design
#### Routes file
The API is mapped from URL to handler in the `app/routes` file: this makes navigating the interface simpler,
and with the associated `conversions/Binders` allows the API to be defined in terms of the problem domain.

In this case, the `common.model.ApiKey` is used to encode the incoming key value: good practice to reduce
the opportunity for possible 'injection' style attack vectors, as the domain object is responsible for protecting
its own state
#### ApiController
Following Play convention, a controller was built to manage the API requests.  The responsibility of the controller
is strictly limted to dealing with HTTP requests and responses, delegating all domain logic to the injected 
`LimitsService`

To differentiate CSV and fixed-format uploads, the controller relies on the correct setting of the `Content-Type` header:
`text/csv` for CSV files and anything else considered as fixed-format (although `text/plain` is preferred for this case).
#### LimitsService
The knowledge of how to process limits files is encapsulated in this service.

This service utilises the `Akka-streams` framework, to split processing into a pipeline of discrete stages, each
responsible for a single task, such as rendering input data into a canonical form, validating the data and 
creating the domain objects. 

This service also runs within its own `ExecutionContext`, not to interfere with the controller's pool (which
could potentially impact API request handling).  The stream supervisor declutters exception processing, allowing
the processing stages to be written to do just their own work.  

In future evolutions, streams also simplifies managing the concurrency aspect of processing, allowing the maximum
number of parallel work-streams to be specified and enforced, and back-pressure to be applied to sources.

A `dataSource` is made available to the controller's body parser, to render incoming data (CSV or fixed-format) into
a canonical key/value map, to allow down-stream processing to remain ignorant of the original format.  It does this
by splitting input lines with a regular-expression that delimits by commas (CSV), or a recursive 'slicer' function,
that divides the line based on the configured column size (fixed-format).
#### Domain Model
Although domain information for this project was trivial, a couple of domain classes were defined, as a logical
place to enforce domain rules.  `CreditLimit` is the prime domain concept, representing the line items in the 
limits files, supported by `java.time.LocalDate`, `BigDecimal` and `PhoneNumber`
#### Security
* Given the proof-of-concept status of the project, authorization/authentication with a simple api key is deemed 
sufficient, rather than a more flexible, OAuth style solution
* As the service is only intended for local deployment, CSRF filters were disabled
* In production, should be made available over HTTPS for privacy / integrity of communications.  HTTP was 
considered adequate fot the proof-of-concept nature of the mission
#### Performance
The sample data set is small, but it is worth considering the implications of loading larger datasets, 
so a streaming approach to uploading the limits files was taken, limiting the memory footprint of the actions
#### Configuration
A limited configuration was provided, specifying the size of the columns of the fixed-file format, but not the 
number or ordering of columns
```
formats {
  headings = ["Name","Address","Postcode","Phone","Limit","Date of birth"]
  fixed {
    columnsWidths = [16,22,9,14,13,8]
  }
}
```

### Dependencies & Libraries
This solution is based on Play! framework 2.6.x, and uses no libraries or frameworks not shipped as part of that environment

## What would I have done differently, given more time?
* Make the service a Play module, to allow it to be incorporated and configured for other applications
* Use an approach such as the IBM [Copybook](https://www.ibm.com/support/knowledgecenter/en/SSLVY3_10.0.0/com.ibm.mdmhs.fstrk.gd.doc/r_Sample_Copybook_Structure.html)
format, if more flexibility is needed to define alternate fixed-format records
* Incrementally return records to reduce memory usage
* Make more aspects of the format processing configurable (e.g., header names, optional extra data items)
* Provide an event-log to permanently store data uploaded: could be valuable as big data in the future, for analytics, etc.
* Internationalise the messages / headings
* Address more edge-cases in the tests
* Provide better error reporting
* Other output formats (e.g., Json) based on request 'Accepts' header

## Running the system
### CI/CD Pipeline
A continuously-deployed instance of this system is available on Heroku (from a mirroered repo). This pipeline includes hooks into Travis-CI (build server), Coveralls (test coverage reporting) and Codacy (code style checker). Substitute that url in the curl commands below for a live demo.

_Note that this running on a free Dyno, so on first invocation (e.g., GET /), expect delays as it builds up steam._

### Prerequisites 
The target language is Scala version 2.12, and uses the build tool sbt 1.2.1.
Clone this repository in a fresh directory:
```git
% git clone git@github.com:sothach/limiter.git
```
In that directory, compile the example with the following command:
```shell
% sbt clean compile
[info] Done compiling.
[success] Total time: 6 s, completed 6-Sep-2018 22:38:12
```

Run the server on the local system, available at [http://localhost:9000](http://localhost:9000)
```shell
% sbt run
```

## Sample usage
### POST limits upload request
Use `curl` to post an upload request (CSV)
```shell
curl -i -H "Content-Type: text/csv;charset=UTF-8" -X POST \
 http://localhost:9000/api/limits?apiKey=eabb12404d141ed6e8ee2193688178cb \
 --upload-file Workbook2.csv
```
Post an upload request (fixed-formay)
```shell
curl -i -H "Content-Type: text/plain;charset=UTF-8" -X POST \
 http://localhost:9000/api/limits?apiKey=eabb12404d141ed6e8ee2193688178cb \
 --upload-file Workbook2.prn
```

## Testing
### Running the tests
Run the test suite to verify correct behaviour.  

From the command line:
```shell
% sbt test
```
### Test Coverage Report
To measure test coverage, this app uses the 'scoverage' SBT plugin.
To create the report, from the command line:
```shell
% sbt coverage test coverageReport
```
#### Status
```sbtshel
[info] Generating scoverage reports...
[info] Written Cobertura report [/Users/roy/workspace/betest/target/scala-2.12/coverage-report/cobertura.xml]
[info] Written XML coverage report [/Users/roy/workspace/betest/target/scala-2.12/scoverage-report/scoverage.xml]
[info] Written HTML coverage report [/Users/roy/workspace/betest/target/scala-2.12/scoverage-report/index.html]
[info] Statement coverage.: 100.00%
[info] Branch coverage....: 100.00%
[info] Coverage reports completed
[info] All done. Coverage was [100.00%]
```

## Author
* [Roy Phillips](mailto:phillips.roy@gmail.com)
