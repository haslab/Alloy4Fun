FROM node:latest

ENV METEOR_ALLOW_SUPERUSER=true
ENV METEOR_DISABLE_OPTIMISTIC_CACHING=1 
ENV ROOT_URL="http://localhost:3000"

RUN curl -k "https://install.meteor.com/" | sh

COPY . /usr/src/app
WORKDIR /usr/src/app

RUN chmod -R 700 /usr/src/app/.meteor/
RUN meteor npm install

EXPOSE 3000
CMD ["meteor","run","--allow-superuser","--settings","settings.json","--port","3000"] 
