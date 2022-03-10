## Thought process

* Docker is the recommended way to run the application. I frequently use docker, but for development I prefer running directly on the host, so I installed sqlite.
* My JVM version is too recent for Gradle, so I updated Gradle from 6.2 to 6.3.
* As I don't want to attempt to charge invoices multiple times, and the invoices need to be charged on a certain first day of the month, I've added the due date field and the `PAYMENT_REJECTED` enum value.
* I set up the sqlite database to be created in the working directory for easier debugging.
* `BillingService` will need a way to get all the due invoices, so I added a query to `AntaeusDal`. An invoice is due if the due date is reached and the invoice is marked `PENDING`.
* I created a REST endpoint to quickly test that the query only returns due invoices.
* I created a scheduler that queries the database for due invoices every hour. If an invoice is created on the same day it can still get processed that same day.
* The scheduler works on a single thread and the calls to the database and payment provider are blocking. This could be considered bad in terms of scalability, but is unlikely to become an issue as low throughput is expected. For greater scalability the calls could be made asynchronous.
* I added error handling to `BillingService`. An invoice may now be marked with the `ERROR` status if there is a problem with currency mismatch or an unknown customer. When either of those two errors occur we won't retry, but for other errors we will.
* I've added MockK tests for `BillingService`. I've also taken a glance at the invoice table to see that it works as intended.

### Closing thoughts

* Invoices can have the `ERROR` status which really doesn't convey much. A way to fix this could be to define several different enum error values such as `CURRENCY_MISMATCH`, but this list of potential errors could never be exhustive and would make the status enum unnecessarily convoluted. Alternatively a string could be added to the table which contains an error message intended for developer debugging.
* There should ideally never be pending invoices that have been due for more than a few hours, and there should ideally never be invoices with the `ERROR` status. When this happens we should know about it. I have used Prometheus for this kind of monitoring and Datadog could probably accomplish the same goal.
* I considered possibly doing something to solve currency mismatches. I think it is ultimately the responsibility of whichever hypothetical external service that generates the invoices to always use the correct currency. If Antaeus still has to deal with currency mismatches then Antaeues should simply notify the external service that there is a mismatch, after which the service could issue a new invoice and cancel the old one.
* The one thing to NOT do is to modify the currency and amount of an invoice. These values should be considered immutable as the customer must never have an older version of an invoice.

## Antaeus
Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!
