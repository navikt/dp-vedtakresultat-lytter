FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:0552cdd6e413cafaeced409aa5d9cff954ea7128864504d26ce0abfd67e63be4

ENV TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dp.vedtakslytter.VedtakslytterKt"]