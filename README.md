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
- POST `/manage-service-metadata` | `/manageservicemetadata`: SOAP endpoint to manage the service registration

> This is the URL suffix to put in `Manage SMPs`

- POST `/manage-business-identifier` | `/manageparticipantidentifier`: SOAP endpoint to manage the identifiers

> This is the URL suffix to put in `Manage participants`

> [!NOTE]
> The 2 urls `/manageservicemetadata`and `/manageparticipantidentifier` were added to be easier to integrate with [phoss-smp](https://github.com/phax/phoss-smp) while not having to change other configurations that the test zone.

## Configuration

There is multiple environment variable that can be set to configure the service:

- `SML_DB_FILE`: The sqlite database location.

- `SML_DNS_SERVER`: The dns server to use to perform the update (default: `127.0.0.1`)
- `SML_DNS_PORT`: The dns server port (default: `53`)

- `SML_DNS_ZONE`: The dns zone to use (default: `europa.eu`)
- `SML_DNS_DOMAIN`: The dns domain to use (default: `acc.edelivery.tech.ec.europa.eu`)
  > If this domain doesn't contains the `SML_DSN_ZONE`, it is automatically append at the end.

- `SML_DNS_KEY_NAME`: The dns key name to use for the update (default: `default`)
- `SML_DNS_KEY_SECRET`: The dns key secret to use for the update (default: `default`)

The application runs on the port 8080 by default, but could be mapped to any other port using docker port binding.
