// Cree une base et un utilisateur dedie par service.
//
// Ce script ne s'execute qu'au premier demarrage, quand le volume de donnees
// est vide. Pour le rejouer : docker compose down -v && docker compose up -d
//
// Le principe est le moindre privilege : chaque utilisateur n'a le droit
// readWrite que sur sa propre base. auth-service ne peut donc pas lire les
// profils scolaires, et content-service ne peut pas lire les mots de passe.

const password = process.env.OJINO_DB_PASSWORD || 'ojino_dev_password';

const services = [
    { database: 'ojino_auth', user: 'auth_service' },
    { database: 'ojino_user', user: 'user_service' },
    { database: 'ojino_content', user: 'content_service' },
    { database: 'ojino_learning', user: 'learning_service' },
    { database: 'ojino_planning', user: 'planning_service' },
    { database: 'ojino_media', user: 'media_service' },
    { database: 'ojino_notification', user: 'notification_service' },
    { database: 'ojino_engagement', user: 'engagement_service' },
    { database: 'ojino_assistant', user: 'assistant_service' },
];

services.forEach(function (service) {
    const target = db.getSiblingDB(service.database);

    target.createUser({
        user: service.user,
        pwd: password,
        roles: [{ role: 'readWrite', db: service.database }],
    });

    // Mongo ne cree une base que lorsqu'elle recoit sa premiere ecriture.
    // Cette collection temoin la fait exister tout de suite, ce qui rend le
    // resultat visible dans mongo-express des le depart.
    target.createCollection('_bootstrap');

    print('Base ' + service.database + ' prete pour l\'utilisateur ' + service.user);
});
