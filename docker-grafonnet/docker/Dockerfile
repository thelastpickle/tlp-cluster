FROM  sparkprime/jsonnet

RUN apk update && apk add git && git clone https://github.com/thelastpickle/grafonnet-lib.git && \
    cd grafonnet-lib && git checkout 80f8b8746ebbf63117e6fc8e75f07fc3900be805

COPY entrypoint.sh /entrypoint.sh
RUN chmod 755 /entrypoint.sh

ENTRYPOINT /entrypoint.sh
