# sml

This repo contains a small poc for an equivalent of the [peppol-sml](https://docs.peppol.eu/edelivery/).
> Sorry, the doc from peppol doesn't have any reference other than pdf files that may disapears

## Why ?

When testing an environment, I do really like to have the full control on everything possible, including the SML.
While looking for any implementation online, I wasn't able to find any, or they were too complicated to setup/relies on components that must be deployed on the own host.

So I came with this solution, a lightweight [ktor](https://ktor.io/) service, that will be sided with a [bind9](https://www.isc.org/bind/) service to perform dns update.

The project is configured to use the test zone from peppol, `acc.edelivery.tech.ec.europa.eu`.

> [!CAUTION]
> It has been mostly tested with [phoss-smp](https://github.com/phax/phoss-smp), and is really not **Production ready**. I didn't care to have a good error handling nor everything that is mendatory for a production service.

## Routes

- GET `/`: Basic page to display what is stored inside the SML.
- POST `/manage-service-metadata`: SOAP endpoint to manage the service registration

> This is the URL suffix to put in `Manage SMPs`

- POST `/manage-business-identifier`: SOAP endpoint to manage the identifiers

> This is the URL suffix to put in `Manage participants`
