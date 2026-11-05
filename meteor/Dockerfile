FROM node:24-slim AS build
ARG METEOR_RELEASE=3.5.4
ENV METEOR_ALLOW_SUPERUSER=true
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates python3 build-essential \
    && rm -rf /var/lib/apt/lists/*
RUN curl -fsSL https://install.meteor.com/?release=${METEOR_RELEASE} | sh
WORKDIR /app
COPY . .
RUN meteor npm install && meteor build /out --directory --server-only
RUN cd /out/bundle/programs/server && npm install --omit=dev

# the runtime must use the same Node version bundled with the Meteor release
FROM node:24-slim
WORKDIR /app
COPY --from=build /out/bundle ./
ENV PORT=3000
EXPOSE 3000
CMD ["node", "main.js"]
