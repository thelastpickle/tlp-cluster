local grafana = import 'grafonnet/grafana.libsonnet';
local dashboard = grafana.dashboard;
local row = grafana.row;
local graphPanel = grafana.graphPanel;
local tablePanel = grafana.tablePanel;
local singleStatPanel = grafana.singlestat;
local textPanel = grafana.text;
local prometheus = grafana.prometheus;
local template = grafana.template;


/*
# HELP selects Generated from Dropwizard metric import (metric=selects, type=com.codahale.metrics.Timer)
# TYPE selects summary
selects{quantile="0.5",} 0.002851058
selects{quantile="0.75",} 0.00331815
selects{quantile="0.95",} 0.004561816000000001
selects{quantile="0.98",} 0.004561816000000001
selects{quantile="0.99",} 0.004561816000000001
selects{quantile="0.999",} 0.004561816000000001
selects_count 9.0
# HELP mutations Generated from Dropwizard metric import (metric=mutations, type=com.codahale.metrics.Timer)
# TYPE mutations summary
mutations{quantile="0.5",} 0.003054039
mutations{quantile="0.75",} 0.0034644000000000003
mutations{quantile="0.95",} 0.011488619
mutations{quantile="0.98",} 0.011488619
mutations{quantile="0.99",} 0.011488619
mutations{quantile="0.999",} 0.011488619
mutations_count 13.0
# HELP errors_total Generated from Dropwizard metric import (metric=errors, type=com.codahale.metrics.Meter)
# TYPE errors_total counter
errors_total 0.0
# HELP populateMutations Generated from Dropwizard metric import (metric=populateMutations, type=com.codahale.metrics.Timer)
# TYPE populateMutations summary
populateMutations{quantile="0.5",} 0.0
populateMutations{quantile="0.75",} 0.0
populateMutations{quantile="0.95",} 0.0
populateMutations{quantile="0.98",} 0.0
populateMutations{quantile="0.99",} 0.0
populateMutations{quantile="0.999",} 0.0
populateMutations_count 0.0
*/

dashboard.new(
  'tlp-stress',
  schemaVersion=14,
  refresh='5s',
  time_from='now-5m',
  editable=true,
  tags=['Cassandra', 'Overview', 'Stress']
)
.addTemplate(
  grafana.template.datasource(
    'PROMETHEUS_DS',
    'prometheus',
    'Prometheus',
    hide='label',
  )
)
.addRow(
    row.new(title="Stress Overview")
    .addPanel(
        singleStatPanel.new(
            "Aggregate Writes / Second",
            description="Aggregate Writes / Second",
            postfix=" writes/s",
            sparklineShow=true,
            timeFrom='30s',
            datasource='$PROMETHEUS_DS',
            valueName="current",
            )
            .addTarget(
                prometheus.target(
                    'sum(irate(mutations_count{job="stress"}[15s]))'
                    )
                )
            )
    .addPanel(
        singleStatPanel.new(
            "Aggregate Reads / Second",
            description="Aggregate Reads / Second",
            postfix=" reads/s",
            sparklineShow=true,
            timeFrom='30s',
            datasource='$PROMETHEUS_DS',
            valueName="current",
            )
            .addTarget(
                prometheus.target(
                    'sum(irate(selects_count{job="stress"}[15s]))'
                    )
                )
            )
    .addPanel(
        singleStatPanel.new(
            "Aggregate Errors / Second",
            description="Aggregate Errors / Second",
            postfix=" errors/s",
            sparklineShow=true,
            timeFrom='30s',
            datasource='$PROMETHEUS_DS',
            valueName="current",
            )
            .addTarget(
                prometheus.target(
                    'sum(irate(errors_total{job="stress"}[15s]))'
                    )
                )
            )
)
.addRow(
    row.new(title="Latency")
    .addPanel(
        graphPanel.new(
              'Write Latency (p99)',
              description='p99 Write Latency (ms)',
              format='short',
              datasource='$PROMETHEUS_DS',
              transparent=true,
              fill=0,
              legend_show=true,
              legend_values=true,
              legend_current=true,
              legend_alignAsTable=true,
              legend_sort='current',
              legend_sortDesc=true,
              shared_tooltip=false,
              decimals=2,
              min=0,
            )
        .addTarget(
            prometheus.target('mutations{quantile="0.99", job="stress"} * 1000',
            legendFormat="{{instance}}"
            )
        )
    )
    .addPanel(
        graphPanel.new(
              'Read Latency (p99)',
              description='p99 Read Latency (ms)',
              format='short',
              datasource='$PROMETHEUS_DS',
              transparent=true,
              fill=0,
              legend_show=true,
              legend_values=true,
              legend_current=true,
              legend_alignAsTable=true,
              legend_sort='current',
              legend_sortDesc=true,
              shared_tooltip=false,
              decimals=2,
              min=0,
            )
        .addTarget(
            prometheus.target('selects{quantile="0.99", job="stress"} * 1000',
                              legendFormat="{{instance}}"
            )
        )
    )

)