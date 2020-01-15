local grafana = import 'grafonnet/grafana.libsonnet';
local dashboard = grafana.dashboard;
local row = grafana.row;
local graphPanel = grafana.graphPanel;
local tablePanel = grafana.tablePanel;
local singleStatPanel = grafana.singlestat;
local textPanel = grafana.text;
local prometheus = grafana.prometheus;
local template = grafana.template;


// used in the single stat panels where higher is better - cache hit rates for example
local reversedColors =[
 '#d44a3a',
 'rgba(237, 129, 40, 0.89)',
 '#299c46',
];

local smallGrid = {
  'w': 4,
  'h': 4
};

local singleStatCachePanel(name, scope) =
    singleStatPanel.new(
              name,
              span=2,
              colors=reversedColors,
              datasource='$PROMETHEUS_DS',
              format='percentunit',
              decimals=0,
              gaugeShow=true,
              gaugeMaxValue=1,
              timeFrom='',
              valueMaps=[
                  {
                    'op': '=',
                    'text': '0',
                    'value': 'null'
                  }
                ],
              thresholds='0.80,0.90',
            )
            .addTarget(
              prometheus.target(
                  'avg(irate(org_apache_cassandra_metrics_cache_count{scope="%(scope)s", name="Hits"}[1m]) / on (instance) irate(org_apache_cassandra_metrics_cache_count{scope="%(scope)s", name="Requests"}[1m]))' % {scope:scope}
              )
            );

local cacheGraphPanel(name, scope) =
    graphPanel.new(name,
              datasource='$PROMETHEUS_DS',
              format='percentunit',
              decimals=1,
            )
            .addTarget(
              prometheus.target(
                  'org_apache_cassandra_metrics_cache_oneminuterate{scope="%(scope)s", name="Hits"} / on (instance) org_apache_cassandra_metrics_cache_oneminuterate{scope="%(scope)s", name="Requests"} ' % {scope:scope},
                  legendFormat='{{instance}}'
              )
            );


dashboard.new(
  'Caches',
  schemaVersion=14,
  refresh='1m',
  time_from='now-15m',
  editable=true,
  tags=['Cassandra', 'Resources', 'Cache'],
)
.addTemplate(
  grafana.template.datasource(
    'PROMETHEUS_DS',
    'prometheus',
    'Prometheus',
    hide='label',
  )
)
.addTemplate(
  template.new(
    'node',
    '$PROMETHEUS_DS',
    'label_values(node_cpu_seconds_total{cpu="0", mode="user", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack"}, node)',
    label='Node',
    refresh='time',
    current='all',
    includeAll=true,
    multi=true,
  )
)

.addRow(
  row.new(title='Quick Stats')
    .addPanel(singleStatCachePanel('Key Cache Hit Rate', 'KeyCache'), smallGrid)
    .addPanel(singleStatCachePanel('Counter Cache Hit Rate', 'CounterCache'), smallGrid)
    .addPanel(singleStatCachePanel('Row Cache Hit Rate', 'RowCache'), smallGrid)
    .addPanel(singleStatCachePanel('Chunk Cache Hit Rate', 'ChunkCache'), smallGrid)
)
.addRow(
    row.new(title='Key Cache')
    .addPanel(cacheGraphPanel('Key Cache', 'KeyCache'))

)
.addRow(
    row.new(title='Counter Cache')
    .addPanel(cacheGraphPanel('Counter Cache', 'CounterCache'))
)
.addRow(
    row.new(title='Row Cache')
    .addPanel(cacheGraphPanel('Row Cache', 'RowCache'))

)
.addRow(
    row.new(title='Chunk Cache')
    .addPanel(cacheGraphPanel('Chunk Cache', 'ChunkCache'))
)
