# Scala interface to NAV Online Invoicing API 2.0

Hungarian Invoicing is a Scala library to interface with NAV Online Invoicing API 2.0.

## Installation

Add Enliven Systems repository:

```
resolvers ++= Seq("Enliven Systems Central" at "https://central.enliven.systems/artifactory/sbt-release/")
```

Add as a dependency:

```
"systems.enliven.invoicing.hungarian" %% "core" % "0.3.2"
```

Don't let the low version number fool you. See Test section for more details.
Please note that currently we only provide Scala 2.12 builds. Drop us a message in case you need
a build for Scala 2.11 or Scala 2.13. Java port is possible. See more info below.

## Prerequisites

You may have to add NAV servers to your Java trust store manually, otherwise you'll get errors
like: `General SSLEngine problem`.
For this we include a script in `/development/import-certificates.sh`. Please fine-tune the
script to your needs, especially by setting your `JAVA_HOME` correctly.

## Test

See section about the configuration. You will have to provide test API credentials for tests to work:
some of them directly connect to NAV servers.

```
sbt test
```

Each build is released with 46+ tests succeeds on our CI. Running tests with a proper configuration
you should see:

```
[info] Tests: succeeded 46, failed 0, canceled 0, ignored 0, pending 0
```

## Usage & features

Create a configuration.

```
import systems.enliven.invoicing.hungarian.core.Configuration

implicit val configuration = new Configuration()
```

Add your own `invoice-hungarian.conf` to the classpath to override any value.

Defaults are the following (`invoice-hungarian.defaults.conf` bundled with the package):

```
invoicing-hungarian {
  connection {
    # The number of Connection actors in the ConnectionPool.
    pool = 6
    # The size of the stash used to save request while exchange tokens are not available.
    stash = 100
    # The maximum number of consequent retries to refresh the exchange token if network error occurs
    # API error will not be retried.
    maxRetry = 10
  }
}
```

Add `invoice-hungarian.conf` with the following content. Please note that these are exemplary
values, do not use them directly!

```
invoicing-hungarian {
  authentication {
    signing-key = "ds-fj92-89r32hADSAKLFH4738FJSAAA"
    exchange-key = "92NASJKASNJK1289"
    login = "932j29h329fhdss"
    password = "g9h8g893fh9dhf29"
  }
  entity {
    tax-number = "25962295"
  }
  request {
    base = "https://api-test.onlineszamla.nav.gov.hu/invoiceService/v2/"
  }
  software {
    identifier = "ENLIVEN25962295207"
    name = "Enliven Systems Invoicing Hungarian"
    version = "0.0.3"
    developer = {
      name = "Enliven Systems Kft."
      contact = "hello@enliven.systems"
      country-code = "HU"
      tax-number = "25962295-2-07"
    }
  }
}
```

Instantiate the API.

```
import systems.enliven.invoicing.hungarian._
import systems.enliven.invoicing.hungarian.api.Api.Protocol.Request.Invoices

import javax.xml.bind.DatatypeConverter

import scala.concurrent.duration._

val invoicing = new Invoicing()
invoicing.awaitInit()
```

or initialize the API asynchronously

```
invoicing.init()
```

Create a few invoices.

```
val invoice: Invoices.Invoice =
	Invoices.Raw(Invoices.Operation.storno, DatatypeConverter.parseBase64Binary("something"))

val validInvoices = (1 to 100).map(_ => invoice)
```

Alternatively you can use other type of invoices, for example directly provide the protocol object
as in `Invoices.Protocol` or use a limited, but self-validating class `Invoices.Smart`. See tests
and code for examples.

Use the synchronous API,

```
invoicing.invoices(Invoices(validInvoices))(10.seconds)
```

or the asynchronous API backed by AKKA.

```
invoicing.invoices(Invoices(validInvoices), 10.seconds)(10.seconds)
```

Please note that you don't have to request exchange tokens. The API will do it for you.

The API uses a connection pool that you may fine tune to get the maximum throughput for
your application.

## Fine tuning

Provide your own `actor.conf` to fine-tune the underlying AKKA actor system, for example:

```
akka {
  actor {
    priority-dispatcher {
      type = "Dispatcher"
      mailbox-type = "systems.enliven.invoicing.hungarian.behaviour.PriorityMailbox"
    }
    blocking-dispatcher {
      type = "Dispatcher"
      executor = "thread-pool-executor"
      thread-pool-executor {
        fixed-pool-size = 6
      }
      throughput = 1
    }
  }
  http.host-connection-pool.max-open-requests = 128
}
```

## Logging

Provide your own logging by adding a `log4j.properties` to your classpath, for example:

```
# Root logger option.
log4j.rootLogger=INFO, stdout

# Direct log messages to STDOUT.
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.Target=System.out
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n
log4j.appender.stdout.encoding=UTF-8
```

## Questions?

Contact us at [hello@enliven.systems](mailto:hello@enliven.systems) or raise an issue on Github.

## Need support?

Please contact us at [hello@enliven.systems](mailto:hello@enliven.systems) or see what we do on
[Enliven Systems](https://enliven.systems).

## Looking for the NAV API in other languages?

- PHP [pzs/nav-online-invoice](https://github.com/pzs/nav-online-invoice)
- Java - interested in Java API? The Scala API can be directly used or ported in Java code. In
case you need help, contact us or raise an issue here.

## Contributing

Pull requests are welcome.
For major changes, please open an issue first to discuss what you would like to change.
Please make sure to update tests as appropriate.

Please note that the proper language to use on our Github pages is English.
