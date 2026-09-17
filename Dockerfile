FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-26@sha256:a9ac261f46aa1291e23e84472ad8bdadfa78ad882768da0526a162ca1c39715f

ENV TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dp.vedtakslytter.VedtakslytterKt"]