FROM  sparkprime/jsonnet

RUN apk update && apk add git && git clone https://github.com/grafana/grafonnet-lib.git

COPY entrypoint.sh /entrypoint.sh
RUN chmod 755 /entrypoint.sh

ENTRYPOINT /entrypoint.sh