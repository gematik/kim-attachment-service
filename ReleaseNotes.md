# Release 1.5.2
# Release 1.5.2

Die aktuelle Version des Kim-Attachment-Service setzt nun die Spezifikation 1.5.2 um. Die
Spezifikation
ist [hier](https://github.com/gematik/api-kim/blob/master/src/openapi/AttachmentService.yaml) zu
finden.

## Änderungen

* Pfadänderung (/attachments/v2.2/*)
* Operation read_MaxMailSize gelöscht
* Umbenennung "Shared-Link" --> "sharedLink"
* Berücksichtigung der Parameter: expires, recipients, messageId
* Diverse Errorcodeänderungen
* BasicAuth für add_Attachment hinzugefügt
* MIME type geändert für add_Attachment

## Quota/Basic-Auth 

Die Basic-Auth und Quota Funktionalität wurde in den Accountmanager ausgelagtert. Dieser bietet eine Schnittstelle wie unter `src/main/resources/api/kim-am-kas-api.yml`. Hieraus kann ein Client mit entsprechender Gegenstelle im Accountmanager erstellt werden.

# Release 1.0.0
# Initial version

## Features

* Max Mail Size
* add Attachment
* read Attachment

First release with Http only. Https will be available in future versions.

