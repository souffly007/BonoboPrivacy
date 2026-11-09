# Bonobo Privacy — 0.1.0 beta

Projet Android Studio Kotlin + Jetpack Compose, package `fr.bonobo.privacy`.

## Ouverture et compilation

1. Décompresser l’archive puis choisir **Open** dans Android Studio à la racine `BonoboPrivacy`.
2. Laisser Android Studio installer/synchroniser les composants demandés (JDK 17, SDK Android 35).
3. Lancer sur un téléphone ou émulateur Android 8+ (API 26).

Le projet impose Java/Kotlin 17 afin d’éviter les erreurs de compatibilité entre cibles JVM.

Le projet contient le Gradle Wrapper. La commande de compilation est :

```bash
./gradlew assembleDebug
```

## Fonctionnalités de cette base

- Accueil sombre bleu/cyan, score calculé à partir des permissions sensibles détectées.
- Inventaire des applications déclarant Microphone, Caméra ou Position.
- Accès direct à la page système d’une application pour gérer ses autorisations.
- Chaque carte Microphone, Caméra et Localisation ouvre sa propre liste d’applications, avec accès direct à leur fiche Android.
- Les applications système Android sont exclues de l’écran principal afin que les chiffres restent utiles.
- Les compteurs et le score reposent sur les autorisations réellement accordées par Android ; une permission seulement déclarée apparaît comme « non autorisée ».
- L’indice est plafonné par famille de capteurs afin qu’une utilisation normale du téléphone ne soit pas assimilée à un risque maximal.
- Mode Silence pédagogique, ouvrant les réglages officiels de confidentialité Android.
- Icône Bonobo intégrée, générée pour ce projet.

L’application déclare `QUERY_ALL_PACKAGES` afin que l’audit puisse examiner les applications installées. Cette autorisation est utile pour un outil de sécurité installé directement, mais elle devra être justifiée ou adaptée aux règles de distribution choisies avant une publication Google Play.

## Limite Android volontairement transparente

Une application ordinaire ne peut pas désactiver le microphone ou la caméra pour les autres applications. Android conserve ce contrôle au système et à l’utilisateur. Bonobo Privacy accompagne ces réglages et rend l’audit compréhensible, sans prétendre intercepter ou bloquer les autres applications.

## Suite recommandée

- Ajouter un historique local des audits (Room).
- Ajouter une liste personnelle « à vérifier / ignorée ».
- Vérifier les permissions réellement accordées, avec les limites de visibilité des applications introduites par Android 11.
- Ajouter FR/EN avant publication.
