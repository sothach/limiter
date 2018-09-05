# BETest

## Instructions

    In this repo, you'll find two files, Workbook2.csv and Workbook2.prn. 
    These files need to be converted to a HTML format by the code you deliver. 
    Please consider your work as a proof of concept for a system that can keep track of credit limits from several sources.

## Assumptions
* All fields are mandatory 
* Provide service via REST interface (POST)
* No persistence
* No UI, just return <html> block

## Approach
### Platform
The Play! framework provides a good starting point for this project: it's convention-over-configuration approach to
URL routes, configuration and HTTP actions simplify the work needed
### Domain
Although domain information for this project was trivial, a couple of domain classes were defined, as a logical
place to enforce domain rules.  `CreditLimit` is the prime domain concept, representing the line items in the 
limits files, supported by `java.time.LocalDate` and `PhoneNumber`
### Security
* Given the proof-of-concept status of the project, a simple api key is deemed sufficient, rather than a more flexible,
OAuth style solution
* As the service is only intended for local deployment, CSRF filters were disabled
### Performance
The sample data is small, but it is worth considering the implications of loading larger datasets, 
so a streaming approach to uploading the limits files was taken, limiting the memory foot-print of the actions
### Configuration
A limited configuration was provided, specifying the size of the columns of the fixed-file format, but not the 
number or ordering of columns
```
formats {
  fixed {
    columnsWidths = [16,22,9,14,13,8]
  }
}
```

## What would I have done differently, given more time?
* Make the service a Play module, to allow it to be incorporated and configured for other application
* Use an approach such as the IBM [Copybook](https://www.ibm.com/support/knowledgecenter/en/SSLVY3_10.0.0/com.ibm.mdmhs.fstrk.gd.doc/r_Sample_Copybook_Structure.html)
format, if more flexibility is needed to define alternate fixed-format records
* Use an event-log to permanently store data uploaded: could be valuable as big data in the future, for analytics, etc.
* Internationalise the messages / headings
* Pipeline this repo into a CI/CD environment (e.g., Travis-CI, Heroku, TeamCity)

## Running locally
Run the server on a local system, with source change monitoring / automatic restarting
```shell
% sbt run
```

## Sample usage
### POST limits upload request
Use `curl` to post an upload request
```shell
curl -i -H "Content-Type: text/csv;charset=UTF-8" -X PUT \
 'http://localhost:9000/api/limits?apiKey=eabb12404d141ed6e8ee2193688178cb&mode=csv' \
 --upload-file Workbook2.csv
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
[info] Written Cobertura report [/Users/roy/workspace/scratch/betest/target/scala-2.12/coverage-report/cobertura.xml]
[info] Written XML coverage report [/Users/roy/workspace/scratch/betest/target/scala-2.12/scoverage-report/scoverage.xml]
[info] Written HTML coverage report [/Users/roy/workspace/scratch/betest/target/scala-2.12/scoverage-report/index.html]
[info] Statement coverage.: 100.00%
[info] Branch coverage....: 100.00%
[info] Coverage reports completed
[info] All done. Coverage was [100.00%]
```


## Author
* [Roy Phillips](mailto:phillips.roy@gmail.com)


