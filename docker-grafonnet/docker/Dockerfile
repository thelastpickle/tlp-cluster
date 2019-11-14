FROM  sparkprime/jsonnet

RUN apk update && apk add git && git clone https://github.com/grafana/grafonnet-lib.git && \
    cd grafonnet-lib && git checkout f3ee1d810858cf556d25f045b53cb0f1fd10b94e

COPY entrypoint.sh /entrypoint.sh
RUN chmod 755 /entrypoint.sh

ENTRYPOINT /entrypoint.sh
