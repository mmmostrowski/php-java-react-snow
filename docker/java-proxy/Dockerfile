FROM php:8.2.3-cli-alpine3.17

WORKDIR /snow/app/

ARG UID=1000
ARG GID=1000
ARG PHP_SNOW_APP_MODE=production

RUN addgroup -S devbox -g ${GID} \
    && adduser -S devbox -G devbox -u ${UID} \
    && apk update \
    && apk upgrade  \
    && apk add bash curl nano vim ncurses \
    && echo 'ls -lah "${@}"' > /usr/local/bin/ll \
    && chmod a+x /usr/local/bin/ll \
    && echo "export PS1='\u@\h:\w\$ '" >> /etc/profile \
    && apk add openjdk17 \
    && curl https://downloads.gradle.org/distributions/gradle-8.0.2-bin.zip -o /opt/gradle.zip \
    && ( cd /opt/ && unzip gradle.zip && rm gradle.zip ) \
    && ln -s /opt/gradle-8.0.2/bin/gradle /usr/local/bin/gradle \
    && ( \
      echo '#!/usr/bin/env bash'; \
      echo '( cd /snow/app/; php snow.php "${@}" )'; \
    ) > /snow/php-cli \
    && chmod a+x /snow/php-cli \
;


# Install X-Debug for develop mode
RUN if [[ "${PHP_SNOW_APP_MODE}" == "develop" ]]; then \
        apk update \
        && apk add busybox-extras autoconf build-base\
        && apk --no-cache add pcre-dev \
        && apk add --update linux-headers \
        && pecl install xdebug-3.2.0 \
        && docker-php-ext-enable xdebug \
        && apk del pcre-dev \
        && ( \
            echo '[xdebug]' \
            && echo 'xdebug.mode=off' \
            && echo 'xdebug.client_host=host.docker.internal' \
            && echo 'xdebug.start_with_request=trigger' \
        ) > /usr/local/etc/php/conf.d/my-xdebug.ini \
        && chmod 666 /usr/local/etc/php/conf.d/my-xdebug.ini; \
    fi \
;


COPY --from=composer:2.2.7 /usr/bin/composer /usr/local/bin/composer
COPY /app/composer.json /snow/app/
COPY /app/lib/ /snow/app/lib/
COPY /app/snow.php /snow/app/snow.php
COPY /app/src/ /snow/app/src/

RUN mkdir -p /data/ \
    && chown devbox:devbox -R /snow/ \
    && chown devbox:devbox -R /data/ \
;


USER devbox

RUN mkdir -p /snow/.pipes/  \
    && composer install \
    && mv /snow/app/vendor/ /data/app-vendor/ \
    && mkdir -p /snow/app/vendor/ \
;


USER root

COPY /java-proxy/ /snow/java-proxy/
RUN chown devbox:devbox -R /snow/ /data/


USER devbox

RUN if [[ "${PHP_SNOW_APP_MODE}" == "production" ]]; then \
        gradle bootJar --project-dir /snow/java-proxy/ \
        && mv /snow/java-proxy/.gradle/ /data/app-gradle/ \
        && mv /snow/java-proxy/build/ /data/app-gradle-build/ \
        && mkdir -p /snow/java-proxy/.gradle/ \
        && mkdir -p /snow/java-proxy/build/; \
    fi \
;

EXPOSE 8080

COPY /docker/java-proxy/entrypoint.sh /
ENTRYPOINT [ "bash", "-l", "/entrypoint.sh"  ]

