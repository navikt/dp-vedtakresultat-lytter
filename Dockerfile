FROM ghcr.io/navikt/baseimages/temurin:18

COPY build/libs/*-all.jar app.jar
