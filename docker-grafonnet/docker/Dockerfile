FROM  sparkprime/jsonnet

RUN apk update && apk add git && git clone https://github.com/thelastpickle/grafonnet-lib.git && \
    cd grafonnet-lib && git checkout jon/sept-rebase

COPY entrypoint.sh /entrypoint.sh
RUN chmod 755 /entrypoint.sh

ENTRYPOINT /entrypoint.sh
