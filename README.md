# Road Watch Kozhikode

Pothole reporting for Kozhikode district.

- `server/`  Spring Boot REST API + the citizen mobile web app (static files)
- `admin-swing/`  Swing admin app (see `admin-swing/README.md`)

## Run locally (VS Code)

1. Install JDK 17+ and Maven.
2. Install the VS Code extensions "Extension Pack for Java" and "Spring Boot Extension Pack".
3. Open the `road-watch-kozhikode` folder in VS Code.
4. In a terminal: `cd server && mvn spring-boot:run`
5. Open http://localhost:8080 for the citizen app.
   (Browsers allow GPS on `localhost`; on a phone you need HTTPS, i.e. the Railway URL.)

Local runs use an in-memory H2 database, so data resets on every restart.
First start seeds the panchayat list and a super admin:
username `superadmin`, password `ChangeMe123!` (change it with SUPERADMIN_PASSWORD).

Testing from outside Kozhikode? Start with `GEOFENCE_ENABLED=false`.

## Try the admin API with curl

    # log in
    curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
      -d '{"username":"superadmin","password":"ChangeMe123!"}'

    # see panchayat ids, then register an admin for one of them (here id 9)
    curl -s localhost:8080/api/panchayats
    curl -s -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" \
      -d '{"username":"kakkodi.admin","password":"secret1234","fullName":"Test Admin","panchayatId":9}'

    # approve it (use the token from the login above)
    curl -s localhost:8080/api/admin/users?pending=true -H "Authorization: Bearer TOKEN"
    curl -s -X POST localhost:8080/api/admin/users/2/approve -H "Authorization: Bearer TOKEN"

## Deploy on Railway

1. Push the `server/` folder to GitHub (make it the repo root, or set Railway's Root Directory to `server`).
2. Railway: New Project -> Deploy from GitHub repo.
3. Add a PostgreSQL service to the same project.
4. In the app service -> Variables, add:
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `PGHOST` = `${{Postgres.PGHOST}}`
   - `PGPORT` = `${{Postgres.PGPORT}}`
   - `PGDATABASE` = `${{Postgres.PGDATABASE}}`
   - `PGUSER` = `${{Postgres.PGUSER}}`
   - `PGPASSWORD` = `${{Postgres.PGPASSWORD}}`
   - `JWT_SECRET` = a long random string (32+ characters)
   - `SUPERADMIN_PASSWORD` = your own strong password
5. Settings -> Networking -> Generate Domain. That HTTPS URL is the citizen app.

## Panchayat list

`server/src/main/resources/panchayats.csv` is loaded on first start. Check it against the
official LSGD Kerala list for Kozhikode district. After launch, the super admin can add
missing ones with `POST /api/admin/panchayats`.
