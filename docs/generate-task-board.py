import json, re
from pathlib import Path

root = Path(__file__).parent
source = (root/'user-stories/P1-chef-de-projet.md').read_text(encoding='utf-8-sig')
stories = {}
for block in re.split(r'(?=^## US-)', source, flags=re.M)[1:]:
    n = int(re.search(r'US-(\d+)', block)[1])
    stories[n] = {'title': block.splitlines()[0].split(' — ')[1], 'story': next(x for x in block.splitlines() if x.startswith('En tant')), 'block': block}

# Each entry separates the feature owner's backend work from Hicham's specialist contribution.
assign = {
8: ('Fatima', 'Créer les migrations, entités, repository et API de création, modification, archivage et liste paginée des clients.', 'Socle tenant et contrôle d’accès commun.'),
9: ('Fatima', 'Créer le modèle dossier, ses migrations et les API CRUD ; valider les champs, le client associé et les changements de statut.', 'Règles de référence inter-cabinet et validation des transactions sensibles.'),
10: ('Fatima', 'Créer les endpoints et DTO d’affectation/retrait ; appeler le moteur de permissions avant chaque opération et persister les affectations autorisées.', 'Implémenter CheckCaseAccess et les règles d’autorisation dossier.'),
11: ('Fatima', 'Implémenter recherche, filtres de statut et pagination dans le repository et l’API, en appliquant le périmètre autorisé.', 'Fournir le filtrage de sécurité commun et revoir les index.'),
12: ('Fatima', 'Exposer la liste paginée de l’historique et brancher les commandes dossier sur le composant d’audit.', 'Fournir le journal append-only et garantir l’intégrité des traces.'),
13: ('Fatima', 'Créer les endpoints de consultation du portail et mapper les dossiers et documents autorisés dans les DTO.', 'Sécuriser la composition interservices et le retrait des accès.'),
14: ('Oumaima', 'Créer les métadonnées, migrations, API de liste et endpoint d’import ; intégrer le composant de stockage et ses validations.', 'Implémenter le stockage privé, la validation du contenu des fichiers et la gestion des échecs d’upload.'),
15: ('Oumaima', 'Créer le modèle de version et les API d’ajout/liste ; utiliser le composant de versionnement pour conserver les fichiers.', 'Garantir la numérotation atomique et les ajouts concurrents sans écrasement.'),
16: ('Oumaima', 'Créer les endpoints partager/révoquer/télécharger et persister le partage après validation des permissions.', 'Implémenter la vérification gRPC des droits et le téléchargement privé.'),
17: ('Oumaima', 'Créer migrations, entité, repository et API d’ajout/lecture des commentaires document ; appliquer la visibilité autorisée.', 'Fournir les règles de visibilité et contrôles d’accès réutilisables.'),
18: ('Oumaima', 'Créer la persistance et les API des risques manuels ; valider description, priorité et page/clause.', 'Revoir l’intégration des permissions document ; aucune analyse automatique.'),
19: ('Fatima', 'Créer entité, migrations et API des commentaires dossier ; valider le texte et appliquer les règles de visibilité.', 'Fournir les règles de visibilité interne/partagée et les contrôles d’accès.'),
20: ('Fatima', 'Créer migrations, CRUD des tâches, filtres et calcul du retard ; valider échéance, statut et responsable autorisé.', 'Implémenter la diffusion des événements et les rappels asynchrones.'),
22: ('Oumaima', 'Créer migrations, API de saisie, liste et correction du temps ; valider les durées et appeler les règles d’audit/verrouillage.', 'Implémenter l’audit des corrections et l’interdiction de modifier les temps facturés.'),
24: ('Oumaima', 'Créer les tarifs cabinet/dossier, migrations et API de consultation/édition ; valider le montant et la devise.', 'Définir la précision monétaire et implémenter la résolution du tarif applicable.'),
25: ('Oumaima', 'Créer les endpoints de sélection des temps et de préparation/lecture du brouillon ; intégrer le moteur de facturation.', 'Implémenter calculs, arrondis, instantanés et protection contre la double facturation.'),
26: ('Oumaima', 'Créer les endpoints de validation, consultation et export ; intégrer le workflow de validation et le générateur PDF.', 'Implémenter gel, PDF et correction traçable ; développer l’écran /factures/:id/correction.'),
27: ('Oumaima', 'Créer la liste filtrée des factures et l’API de déclaration payé/non payé avec audit et rôle requis.', 'Fournir les composants de contrôle de rôle et d’audit ; aucun paiement bancaire réel.'),
28: ('Fatima', 'Créer les requêtes de statistiques dossiers/tâches et leurs endpoints ; intégrer la synthèse dans le tableau de bord.', 'Composer les indicateurs interservices via le BFF avec délais et dégradation contrôlée.')
}
deps = {8:[2,7],9:[8],10:[9,7],11:[9,10],12:[9,36],13:[10,16],14:[9,7],15:[14],16:[14,10],17:[14,16],18:[14],19:[9,10],20:[9,10],22:[9],24:[3,9],25:[22,24],26:[25],27:[26],28:[11,20,22,27]}
rows=[]
for filename in ['P2-dossiers-collaboration.md','P3-documents-facturation.md']:
    text=(root/'user-stories'/filename).read_text(encoding='utf-8-sig')
    for block in re.split(r'(?=^## US-)',text,flags=re.M)[1:]:
        n=int(re.search(r'US-(\d+)',block)[1]); owner,back,special=assign[n]
        ui=re.findall(r'\*\*US-\d+\.UI-\d+\*\* — (.*)',block)[:3]
        ui=[re.sub(r'(fourni(?:e|s|es)?|préparé(?:e|s|es)?)(?: par le chef)?', 'du module',s) for s in ui]
        screen=block.split('**Écrans à développer**')[1].split('**Sous-stories')[0].strip()
        screens=[s[2:] for s in screen.splitlines() if s.startswith('- ')]
        rows.append(dict(id=f'US-{n:02}',owner=owner,title=stories[n]['title'],story=stories[n]['story'],phase=2 if n in [8,9,11,14,18,20,22,24] else 3,screens=screens,deps=[f'US-{d:02}' for d in deps[n]],tasks=[('Backend',back),('Intégration','Développer les DTO, le mapping et les handlers du module selon les contrats REST/gRPC validés ; créer ses appels API et hooks React.' )]+[('Frontend',s) for s in ui]+[('Tests','Tester les API (cas valide, validation et accès refusé) et le parcours écran → serveur → base de données.')],support=special))

for n,s in stories.items():
    if n in assign: continue
    tasks=re.findall(r'\*\*US-\d+\.TECH-\d+\*\* — (.*)',s['block'])
    tasks=[t.replace('Concevoir tous les contrats protobuf, routes REST du BFF, erreurs et pagination.','Définir et valider avec les propriétaires les contrats protobuf, REST, erreurs et pagination.').replace('Générer les stubs et implémenter les clients/serveurs gRPC.','Configurer la génération des stubs, les intercepteurs et le transport gRPC ; fournir un exemple de handler.').replace('Préparer les fonctions frontend typées et les hooks métier consommés par Fatima/Oumaima.','Fournir le client HTTP commun ; chaque propriétaire développe ses fonctions API et hooks métier.').replace('Implémenter personnellement les vérifications tenant, RBAC et permissions métier dans tous les services.','Implémenter les composants tenant/RBAC communs ; chaque propriétaire les applique dans ses endpoints.').replace('Corriger les défauts backend/intégration et coordonner les corrections d’interface avec leurs propriétaires.','Corriger les défauts transversaux ; chaque propriétaire corrige son frontend et son backend.').replace('Confirmer Fatima sur les interfaces dossiers/collaboration et Oumaima sur les interfaces documents/facturation.','Confirmer Fatima sur le frontend/backend dossiers-collaboration et Oumaima sur le frontend/backend documents-facturation.').replace('Implémenter personnellement audit des actions sensibles et suppression logique dans les services concernés.','Fournir les composants d’audit et de suppression logique ; coordonner leur intégration dans les services métier.') for t in tasks]
    if n==1: tasks.append('Configurer le service registry, la gateway et le load balancing ; vérifier le démarrage avec Docker Compose.')
    screens=s['block'].split('**Écrans à développer**')[1].strip().splitlines()
    rows.append(dict(id=f'US-{n:02}',owner='Hicham',title=s['title'],story=s['story'],phase=1 if n in [1,2,3,4,5,6,7,30,31,35,36] else (4 if n in [32,33,34] else 3),screens=[t[2:] for t in screens if t.startswith('- ')],deps=({'US-23':['US-22'],'US-21':['US-02'],'US-32':['US-13','US-26','US-28'],'US-34':['US-32','US-33']}).get(f'US-{n:02}',[]),tasks=[('Pilotage' if n in [34,35] else 'Technique',t) for t in tasks],support=''))
for n,(owner,_,special) in assign.items():
    rows.append(dict(id=f'US-{n:02} · expertise',owner='Hicham',title=stories[n]['title'],story=f'Contribution technique à la fonctionnalité portée par {owner}.',phase=2 if n not in [25,26,28] else 3,screens=['Correction de facture — /factures/:id/correction'] if n==26 else [],deps=['US-02','US-07'],tasks=[('Expertise',special),('Intégration',f'Valider le contrat avec {owner}, fournir un exemple et tester ensemble l’intégration.')],support=''))
rows.sort(key=lambda s:(s['phase'],s['id']))
template=(root/'task-board-template.html').read_text(encoding='utf-8')
(root/'LAWFLUX-taches-equipe.html').write_text(template.replace('__DATA__',json.dumps(rows,ensure_ascii=False)),encoding='utf-8')
print(f'{len(rows)} fiches générées, 36 user stories couvertes.')
