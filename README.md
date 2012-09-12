# SDM4 importer til autorisationsregister

## Igang
Autorisationsimporteren er af typen der bruger en AuditingPersister
Læs stamdata dokumentation der ligger i SDM-Core projektet inden dette projekt bygges.
Se https://github.com/trifork/sdm4-core/tree/sdm-core-4.0/doc

For at køre integrationstests, kræves en opsætning som beskrevet i guide til udviklere

Klon repo med ```git clone https://github.com/trifork/sdm4-autorisationimporter.git```.

## Konfiguration
Der er følgende importer-specifikke konfigurations-properties

*  ``spooler.autorisationimporter.max.allowed.reduction``
  angiver hvor mange autorisationer, der må være færre end ved sidste import
  Default-værdi: 10
  Eksempel: Hvis der er 200.000 autorisationer i databasen, og en import-fil (fx ved en fejl)
  indeholder 100.000 autorisationer, afvises import-filen
  