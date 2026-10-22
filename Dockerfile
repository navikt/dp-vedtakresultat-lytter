FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-27@sha256:689d87058a37ee3fccbd79e526c9535789926cdf43746cd988d087989baaa22c

ENV TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dp.vedtakslytter.VedtakslytterKt"]