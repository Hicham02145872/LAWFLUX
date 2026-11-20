# Load balancing et découverte de services

Le BFF appelle `load-balancer` (Envoy) sur quatre ports internes. Envoy utilise `ROUND_ROBIN` pour choisir une instance par appel gRPC. `discovery-service` (Consul) recense les instances ; `discovery-registrar` interroge le DNS interne Docker et synchronise leurs adresses dans Consul.

```text
Frontend → gateway-bff → load-balancer (Envoy) → service métier
                               ↑
                         Consul DNS
                               ↑
                   discovery-registrar → DNS Docker
```

Les ports Envoy sont 9101 (identité), 9102 (dossiers), 9103 (documents) et 9104 (facturation). Les services n'exposent pas leur port gRPC sur l'hôte. Le BFF utilise `pick_first` vers l'unique Envoy ; la répartition est faite par Envoy.

L'interface de diagnostic Envoy est liée à `127.0.0.1:9901` sur l'hôte. Elle permet de consulter les hôtes découverts via `/clusters?format=json`.

## Démarrage

```powershell
docker compose -f compose.yaml -f compose.load-balancing.yaml --profile apps up -d --build
docker compose -f compose.yaml -f compose.load-balancing.yaml --profile apps ps
```

Le fichier complémentaire démarre trois instances de `case-service`. Consul est disponible localement sur `http://localhost:8500`. Les services enregistrés sont `lawflux-identity`, `lawflux-cases`, `lawflux-documents` et `lawflux-billing`.

La résolution DNS Consul est rafraîchie toutes les deux secondes par Envoy. Elle utilise l'adresse fixe `172.30.55.10` sur le réseau Compose `172.30.55.0/24` : si ce sous-réseau est déjà utilisé, il faut le changer dans `compose.yaml` et `infra/envoy/envoy.yaml`.

Le registrar n'accède pas au socket Docker et tourne sans privilèges. Consul fonctionne en mode `-dev` sans authentification ; il n'est pas destiné à la production.

## Vérification effectuée

Trois instances `case-service` ont été démarrées et déclarées saines dans Consul. Envoy a découvert leurs trois adresses. Lors d'un essai avec 12 appels gRPC `ServiceProbe/Ping`, les 12 appels ont réussi et Envoy a compté 4 appels par instance. Le port gRPC local utilisé pour cet essai était temporaire et a été retiré ensuite.

Un RPC gRPC en streaming reste lié à l'instance choisie jusqu'à sa fin. En cas de panne d'une instance, un appel en cours peut échouer ; aucune écriture n'est rejouée automatiquement.
