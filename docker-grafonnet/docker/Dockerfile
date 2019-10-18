FROM  sparkprime/jsonnet

RUN apk update && apk add git && git clone https://github.com/grafana/grafonnet-lib.git && \
    cd grafonnet-lib && git checkout 47db72da03fc4a7a0658a87791e13c3315a3a252

COPY entrypoint.sh /entrypoint.sh
RUN chmod 755 /entrypoint.sh

ENTRYPOINT /entrypoint.sh
