## autorisationimporter 4.1
*  NSPSUPPORT-67: Indført konfigurerbar tærskel for, hvor mange færre autorisationer en import-fil må indeholde
   sammenlignet med antal autorisationer i databasen
*  Lavet tjek, der sikrer at der kun gives én fil til importeren ad gangen.
   Den eksisterende kode tillader mere end én fil, men gør ikke noget for at importere dem i rigtig rækkefølge i forhold til deres timestamps, så det vil kunne fejle.

## autorisationimporter 4.2
*  NSPSUPPORT-103: Databaseskema kan nu køres på eksisterende sdm3-skema uden runtime-fejl
