local grafana = import 'grafonnet/grafana.libsonnet';
local dashboard = grafana.dashboard;
local row = grafana.row;
local graphPanel = grafana.graphPanel;
local tablePanel = grafana.tablePanel;
local singleStatPanel = grafana.singlestat;
local textPanel = grafana.text;
local prometheus = grafana.prometheus;
local template = grafana.template;

dashboard.new(
  'TLP - Cassandra Overview',
  schemaVersion=14,
  refresh='1m',
  time_from='now-3h',
  editable=true,
  tags=['Cassandra', 'TLP', 'tlp', 'C*', 'The Last Pickle', 'Overview'],
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
    'environment',
    '$PROMETHEUS_DS',
    'label_values(org_apache_cassandra_metrics_clientrequest_oneminuterate, environment)',
    label='Environment',
    refresh='time',
  )
)
.addTemplate(
  template.new(
    'cluster',
    '$PROMETHEUS_DS',
    'label_values(org_apache_cassandra_metrics_clientrequest_oneminuterate{environment="$environment"}, cluster)',
    label='Cluster',
    refresh='time',
  )
)
.addTemplate(
  template.new(
    'datacenter',
    '$PROMETHEUS_DS',
    'label_values(org_apache_cassandra_metrics_clientrequest_oneminuterate{environment="$environment", cluster="$cluster"}, datacenter)',
    label='Datacenter',
    refresh='time',
    includeAll=true,
    multi=true,
  )
)
.addTemplate(
  template.new(
    'rack',
    '$PROMETHEUS_DS',
    'label_values(org_apache_cassandra_metrics_clientrequest_oneminuterate{environment="$environment", cluster="$cluster", datacenter=~"$datacenter"}, rack)',
    label='Rack',
    refresh='time',
    includeAll=true,
    multi=true,
  )
)
.addTemplate(
  template.new(
    'node',
    '$PROMETHEUS_DS',
    'label_values(org_apache_cassandra_metrics_clientrequest_oneminuterate{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack"}, node)',
    label='Node',
    refresh='time',
    current='all',
    includeAll=true,
    multi=true,
  )
)
.addRow(
  row.new(title='Nodes Status')
  .addPanel(
    textPanel.new(
      'Nodes Status per Protocol',
      description='Nodes being up or down - considering the activity of distinct protocols',
      datasource='Prometheus',
      transparent=true,
      mode='html',
      content='
        <br/>
        <br/>
        <center><h2>Nodes Up (Gossip Activity): <span id="gossip_up">0</span>/<span id="gossip_total">0</span></h2><center>
        <center><h2>Nodes Up (Native Client Activity): <span id="native_up">0</span>/<span id="native_total">0</span></h2><center>
        <center><h2>Nodes Reporting Metics: <span id="reporting_up">0</span>/<span id="reporting_total">0</span></h2><center>

        <script type="text/javascript">
        var nodeStatusTimer = setTimeout(function(){console.log("init");}, 1);

        function countNodesInUpState(nodeArray) {
            var upCount = 0;
            for (var i = 0; i < nodeArray.length; i+=1) {
                /*
                console.log("i --> ", i)
                console.log("upCount --> ", upCount)
                console.log("nodeArray --> ", nodeArray)
                console.log("nodeArray.legnth --> ", nodeArray.length)
                console.log("nodeArray[i].childNodes --> ", nodeArray[i].childNodes)
                */
                // Get the node state either 1 or 0 from the row data. Skip every node with an
                // odd index in the array, because it only has the node IP.
                // Increment the count for every node that is up. Sometimes that value may
                // be just less than 1, so allow an "up" status to be 0.5 and above.
                if (Number(nodeArray[i].childNodes[1].nodeValue) >= 0.5) {
                   upCount = upCount + 1;
                }
            }
            return upCount;
        }

        function updatePanel(){
            var panelReporting = $("span:contains(\'Nodes Collecting Metrics Successfully\')").parents("div.react-grid-item")
            var tableReporting = $("span:contains(\'Nodes Collecting Metrics Successfully\')").parents("grafana-panel").find("tbody tr td");
            var nodesReportingTotal = $("span:contains(\'Nodes Collecting Metrics Successfully\')").parents("grafana-panel").find("tbody tr").length;
            var reportingUp = countNodesInUpState(tableReporting);

            var panelGossip = $("span:contains(\'Gossip Activity\')").parents("div.react-grid-item")
            var tableGossip = $("span:contains(\'Gossip Activity\')").parents("grafana-panel").find("tbody tr td");
            var nodesGossipTotal = $("span:contains(\'Gossip Activity\')").parents("grafana-panel").find("tbody tr").length;
            var gossipUp = countNodesInUpState(tableGossip);

            var panelNative = $("span:contains(\'Native Client Activity\')").parents("div.react-grid-item")
            var tableNative = $("span:contains(\'Native Client Activity\')").parents("grafana-panel").find("tbody tr td");
            var nodesNativeTotal = $("span:contains(\'Native Client Activity\')").parents("grafana-panel").find("tbody tr").length;
            var nativeUp = countNodesInUpState(tableNative);

            $("#reporting_up").html(reportingUp);
            $("#reporting_total").html(nodesReportingTotal);
            $("#gossip_up").html(gossipUp);
            $("#gossip_total").html(nodesGossipTotal);
            $("#native_up").html(nativeUp);
            $("#native_total").html(nodesNativeTotal);
            panelReporting.hide();
            panelGossip.hide();
            panelNative.hide();

            if(reportingUp<nodesReportingTotal){
                    $("#reporting_up").attr("style","color:red");
            } else {
                    $("#reporting_up").attr("style","color:lightgreen");
            }
            if(gossipUp<nodesGossipTotal){
                    $("#gossip_up").attr("style","color:red");
            } else {
                    $("#gossip_up").attr("style","color:lightgreen");
            }
            if(nativeUp<nodesNativeTotal){
                    $("#native_up").attr("style","color:red");
            } else {
                    $("#native_up").attr("style","color:lightgreen");
            }
        }

        $(document).ready(function() {
                nodeStatusTimer = setInterval(updatePanel, 1);
        });

        $("span:contains(\'Nodes Collecting Metrics Successfully\')").parents("grafana-panel").find("tbody tr td").bind("DOMSubtreeModified",function(){
                clearTimeout(nodeStatusTimer);
                nodeStatusTimer = setTimeout(updatePanel,1000);
        });
        </script>',
    )
  )
  .addPanel(
    tablePanel.new(
      'Nodes Up (Gossip Activity)',
      description='Nodes being up or down from an internal perspective, based on Gossip Activity',
      datasource='Prometheus',
      transform='timeseries_aggregations',
      transparent=true,
      span=1,
      styles=[
        {
          "alias": "Node",
          "colorMode": null,
          "colors": [
            "rgba(245, 54, 54, 0.9)",
            "rgba(237, 129, 40, 0.89)",
            "rgba(50, 172, 45, 0.97)"
          ],
          "dateFormat": "YYYY-MM-DD HH:mm:ss",
          "decimals": 2,
          "mappingType": 1,
          "pattern": "Metric",
          "preserveFormat": true,
          "sanitize": true,
          "thresholds": [],
          "type": "string",
          "unit": "short"
        },
        {
          "alias": "Gossip Up?",
          "colorMode": "row",
          "colors": [
            "rgba(245, 54, 54, 0.9)",
            "rgba(237, 129, 40, 0.89)",
            "rgba(50, 172, 45, 0.97)"
          ],
          "dateFormat": "YYYY-MM-DD HH:mm:ss",
          "decimals": 0,
          "link": false,
          "mappingType": 1,
          "pattern": "Current",
          "thresholds": [
            "0.1",
            "0.9"
          ],
          "type": "number",
          "unit": "short"
        }
      ],
      columns=[
        {
          "text": "Current",
          "value": "current"
        }
      ],
      sort={
        "col": 1,
        "desc": false
      }
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster, datacenter, rack, node) (changes(org_apache_cassandra_metrics_threadpools_value{scope="GossipStage", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))',
        legendFormat='{{node}}',
        instant=false
      )
    )
  )
  .addPanel(
    tablePanel.new(
      'Nodes Up (Native Client Activity)',
      description='Nodes being up or down from an internal perspective, based on native client activities',
      datasource='Prometheus',
      transform='timeseries_aggregations',
      transparent=true,
      span=1,
      styles=[
        {
          "alias": "Node",
          "colorMode": null,
          "colors": [
            "rgba(245, 54, 54, 0.9)",
            "rgba(237, 129, 40, 0.89)",
            "rgba(50, 172, 45, 0.97)"
          ],
          "dateFormat": "YYYY-MM-DD HH:mm:ss",
          "decimals": 2,
          "mappingType": 1,
          "pattern": "Metric",
          "preserveFormat": true,
          "sanitize": true,
          "thresholds": [],
          "type": "string",
          "unit": "short"
        },
        {
          "alias": "Native Up?",
          "colorMode": "row",
          "colors": [
            "rgba(245, 54, 54, 0.9)",
            "rgba(237, 129, 40, 0.89)",
            "rgba(50, 172, 45, 0.97)"
          ],
          "dateFormat": "YYYY-MM-DD HH:mm:ss",
          "decimals": 0,
          "link": false,
          "mappingType": 1,
          "pattern": "Current",
          "thresholds": [
            "0.1",
            "0.9"
          ],
          "type": "number",
          "unit": "short"
        }
      ],
      columns=[
        {
          "text": "Current",
          "value": "current"
        }
      ],
      sort={
        "col": 1,
        "desc": false
      }
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster, datacenter, rack, node) (changes(org_apache_cassandra_metrics_threadpools_value{scope="Native-Transport-Requests", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))',
        legendFormat='{{node}}',
        instant=false
      )
    )
  )
  .addPanel(
    tablePanel.new(
      'Nodes Collecting Metrics Successfully',
      description='Nodes being up or down - For now uses "up" metric, that only says if we could scrape metrics or not. To be improved',
      datasource='Prometheus',
      transform='timeseries_aggregations',
      transparent=true,
      span=1,
      styles=[
        {
          "alias": "Node",
          "colorMode": null,
          "colors": [
            "rgba(245, 54, 54, 0.9)",
            "rgba(237, 129, 40, 0.89)",
            "rgba(50, 172, 45, 0.97)"
          ],
          "dateFormat": "YYYY-MM-DD HH:mm:ss",
          "decimals": 2,
          "mappingType": 1,
          "pattern": "Metric",
          "preserveFormat": true,
          "sanitize": true,
          "thresholds": [],
          "type": "string",
          "unit": "short"
        },
        {
          "alias": "Up?",
          "colorMode": "row",
          "colors": [
            "rgba(245, 54, 54, 0.9)",
            "rgba(237, 129, 40, 0.89)",
            "rgba(50, 172, 45, 0.97)"
          ],
          "dateFormat": "YYYY-MM-DD HH:mm:ss",
          "decimals": 0,
          "link": false,
          "mappingType": 1,
          "pattern": "Current",
          "thresholds": [
            "0.1",
            "0.9"
          ],
          "type": "number",
          "unit": "short"
        }
      ],
      columns=[
        {
          "text": "Current",
          "value": "current"
        }
      ],
      sort={
        "col": 1,
        "desc": false
      }
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster, datacenter, rack, node) (up{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='{{node}}',
        instant=true
      )
    )
  )
)
.addRow(
  row.new(title='Cluster Requests (Coordinator Perspective)',)
  .addPanel(
    graphPanel.new(
      'Request Throughputs',
      description='Coordinator level read and write count - Max of each 1m rate for each operation type',
      format='rps',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'sum by (scope) (org_apache_cassandra_metrics_clientrequest_oneminuterate{scope=~"Read|Write|CASRead|CASWrite|RangeSlice|ViewRead|ViewWrite", name=~"Latency|ViewWriteLatency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='{{scope}} Request Rate',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Error throughputs',
      description='Timeouts, Failures, Unavailable Rates for each operation type',
      format='rps',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'sum by (name) (org_apache_cassandra_metrics_clientrequest_oneminuterate{scope=~"Read|Write|CASRead|CASWrite|RangeSlice|ViewRead|ViewWrite", name!~"Latency|ViewWriteLatency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='{{name}} Requests ',
      )
    )
  )
  .addPanel(
    singleStatPanel.new(
      'Read / Write Distribution',
      description='Part of reads in the total of standard requests (Reads+Writes). CAS, Views, ... operations are ignored.',
      format='percentunit',
      datasource='Prometheus',
      transparent=true,
      postfix=' Reads',
      postfixFontSize='30%',
      valueFontSize='30%',
      valueName="current",
      decimals=2,
      thresholds='0.25,0.5,0.75',
      timeFrom='1m',
      colors=[
        "#DEB6F2",
        "#CA95E5",
        "#8F3BB8"
      ],
      gaugeShow=true,
      gaugeMinValue=0,
      gaugeMaxValue=1,
      gaugeThresholdLabels=true,
      gaugeThresholdMarkers=false,
      sparklineFillColor='rgba(31, 118, 189, 0.18)',
      sparklineFull=false,
      sparklineLineColor='#FFB357',
      sparklineShow=false
    )
    .addTarget(
      prometheus.target(
        'sum by (scope)(org_apache_cassandra_metrics_clientrequest_oneminuterate{scope="Read", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}) / ignoring (scope) (sum by (scope)(org_apache_cassandra_metrics_clientrequest_oneminuterate{scope="Read", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}) + ignoring (scope) sum by (scope)(org_apache_cassandra_metrics_clientrequest_oneminuterate{scope="Write", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}))',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Read Latency',
      description='Read latency maximum for coordinated reads',
      format='µs',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster, datacenter) (org_apache_cassandra_metrics_clientrequest_99thpercentile{scope="Read", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='Max 99th percentile for {{datacenter}}',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Write Latency',
      description='Write latency maximum for coordinated reads',
      format='µs',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster, datacenter) (org_apache_cassandra_metrics_clientrequest_99thpercentile{scope="Write", name="Latency", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='Max 99th percentile for {{datacenter}}',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Other Latencies',
      description='Other latencies on p99 for coordinated requests',
      format='µs',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'max by (operation) (org_apache_cassandra_metrics_clientrequest_99thpercentile{operation!~"write|read", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
      )
    )
  )
)
.addRow(
  row.new(title='Data Status')
  .addPanel(
    tablePanel.new(
      'Disk Space Usage',
      description='Disks space used ordered (fullest disks first)',
      datasource='Prometheus',
      transform='timeseries_aggregations',
      transparent=true,
      styles=[
        {
          "alias": "Node",
          "colorMode": null,
          "colors": [
            "rgba(245, 54, 54, 0.9)",
            "rgba(237, 129, 40, 0.89)",
            "rgba(50, 172, 45, 0.97)"
          ],
          "dateFormat": "YYYY-MM-DD HH:mm:ss",
          "decimals": 2,
          "mappingType": 1,
          "pattern": "Metric",
          "preserveFormat": true,
          "sanitize": true,
          "thresholds": [],
          "type": "string",
          "unit": "short"
        },
        {
          "alias": "% Disk Space Used",
          "colorMode": "row",
          "colors": [
            "rgba(50, 172, 45, 0.97)",
            "rgba(237, 129, 40, 0.89)",
            "rgba(245, 54, 54, 0.9)"
          ],
          "dateFormat": "YYYY-MM-DD HH:mm:ss",
          "decimals": 2,
          "link": false,
          "mappingType": 1,
          "pattern": "Current",
          "thresholds": [
            "0.5",
            "0.75",
          ],
          "type": "number",
          "unit": "percentunit"
        }
      ],
      columns=[
        {
          "text": "Current",
          "value": "current"
        }
      ],
      sort={
        "col": 1,
        "desc": true
      }
    )
    .addTarget(
      prometheus.target(
        '(1-(node_filesystem_avail_bytes/node_filesystem_size_bytes))',
        legendFormat='{{node}} --> {{mountpoint}}',
        instant=true
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Total Data Size',
      description='Sum of the sizes of the data on distinct nodes',
      format='bytes',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'sum by (environment, cluster, datacenter, rack, node)
        (org_apache_cassandra_metrics_table_value{name="LiveDiskSpaceUsed", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}        )',
        legendFormat='Sum live data size - nodes in cluster: {{cluster}}',
      )
    )
    .addTarget(
      prometheus.target(
        'sum by (environment, cluster, datacenter, rack, node)
        (org_apache_cassandra_metrics_table_value{name="TotalDiskSpaceUsed", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='Sum total data size - nodes in cluster: {{cluster}}',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'SSTable Count',
      description='SSTable Count Max and Average per node and table',
      format='short',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'max by (keyspace, table) (org_apache_cassandra_metrics_table_value{name="LiveSSTableCount", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='Maximum number of SSTables for table: {{keyspace}}.{{table}}',
      )
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster, datacenter) (org_apache_cassandra_metrics_table_value{name="LiveSSTableCount", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='Maximum number of SSTables per node in datacenter: {{cluster}}-{{datacenter}}',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Pending Compactions',
      description='Pending compactions per node',
      format='short',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
      min=0,
      bars=true,
      lines=false,
      stack=true,
    )
    .addTarget(
      prometheus.target(
        'sum by (table) (org_apache_cassandra_metrics_table_value{name="PendingCompactions", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
      )
    )
  )
)
.addRow(
  row.new(title='Cassandra Internals',)
  .addPanel(
    graphPanel.new(
      'Pending Messages',
      description='Pending threads rate summed by pool',
      format='short',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'sum by (scope) (org_apache_cassandra_metrics_threadpools_value{name="PendingTasks", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
      )
    )
  )
 .addPanel(
    graphPanel.new(
      'Dropped Messages',
      description='Dropped messages rate summed by message type',
      format='short',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
      min=0,
    )
    .addTarget(
      prometheus.target(
        'sum by (scope) (org_apache_cassandra_metrics_droppedmessage_oneminuterate{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
      )
    )
  )
)
.addRow(
  row.new(title='Hardware / Operating System',)
 .addPanel(
    graphPanel.new(
      'CPU Utilization',
      description='Maximum CPU utilisation (max 100%)',
      format='percent',
      datasource='Prometheus',
      transparent=true,
      fill=1,
      legend_show=true,
      shared_tooltip=false,
      percentage=true,
      min=0,
      max=105,
    )
    .addTarget(
      prometheus.target(
        '(1 - min by (mode) (rate(node_cpu_seconds_total{mode="idle", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m])))',
        legendFormat='Maximum CPU utilisation',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Unix Load',
      description='Unix load per node',
      format='short',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster, datacenter, rack, node) (node_load1{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='Maximum Load (1m rate)',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Memory Utilisation',
      description='Maximum Memory allocated per usage (worst node) - excludes caches, buffers, etc',
      format='bytes',
      datasource='Prometheus',
      transparent=true,
      legend_show=true,
      shared_tooltip=false,
      fill=1,
      linewidth=2,
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster, datacenter, rack, node)
        (node_memory_MemTotal_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        - node_memory_MemFree_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        - node_memory_Buffers_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        - node_memory_Cached_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        - node_memory_SwapCached_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        - node_memory_Slab_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        - node_memory_PageTables_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        - node_memory_VmallocUsed_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}
        )',
        legendFormat='Maximum memory used for nodes in cluster: {{cluster}}',
      )
    )
    .addTarget(
      prometheus.target(
        'min by (environment, cluster, datacenter, rack, node)
        (node_memory_MemTotal_bytes{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='Minimum amount of memory available for nodes in cluster: {{cluster}}',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Disk IO Wait',
      description='Equivalent to r_await and w_await from `iostat -x` - Building help, see: https://www.robustperception.io/mapping-iostat-to-the-node-exporters-node_disk_-metrics',
      format='percent',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster, datacenter, rack, node)
        (100 * rate(node_disk_read_time_seconds_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m])
        / rate(node_disk_reads_completed_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m]))',
        legendFormat='Max r_await in: {{cluster}}',
      )
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster, datacenter, rack, node)
        (100 * rate(node_disk_write_time_seconds_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m])
        / rate(node_disk_writes_completed_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m]))',
        legendFormat='Max w_await in: {{cluster}}',
      )
    )
    .addTarget(
    #TODO Not working :(
      prometheus.target(
        'max by (environment, cluster, datacenter, rack, node)
        (100 *
          (rate(node_disk_read_time_seconds_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m])
          + rate(node_disk_write_time_seconds_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m]))
        ) / (
          (rate(node_disk_reads_completed_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m])
          + rate(node_disk_writes_completed_total{device=~".*", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[5m]))
        )',
        legendFormat='Max await in: {{cluster}}',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Network I/O',
      description='Network In and Out per node',
      format='bytes',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
      bars=true ,
    )
    .addTarget(
      prometheus.target(
        'sum by (environment, cluster) (rate(node_network_transmit_bytes_total{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))',
        legendFormat='Sum of Network Outgoing, cluster: {{cluster}}',
      )
    )
    .addTarget(
      prometheus.target(
        'sum by (environment, cluster) (-1 * (rate(node_network_receive_bytes_total{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m])))',
        legendFormat='Sum of Network Incoming, cluster: {{cluster}}',
      )
    )
  )
)
.addRow(
  row.new(title='JVM / Garbage Collection',)
  .addPanel(
    graphPanel.new(
      'Garbage Collection Time',
      description='Garbage Collection duration',
      format='s',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'max by (gc, node) (rate(jvm_gc_collection_seconds_sum{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))',
        legendFormat='Maximum GC duration per minute for {{gc}}',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'Garbage Collection Count',
      description='Garbage Collection Count',
      format='short',
      datasource='Prometheus',
      transparent=true,
      fill=0,
      legend_show=true,
      shared_tooltip=false,
    )
    .addTarget(
      prometheus.target(
        'max by (gc, node) (rate(jvm_gc_collection_seconds_count{environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}[1m]))',
        legendFormat='Maximum GC count per minute for {{gc}}',
      )
    )
  )
  .addPanel(
    graphPanel.new(
      'JVM Heap Memory Utilisation',
      description='Maximum JVM Heap Memory size (worst node)',
      format='bytes',
      datasource='Prometheus',
      transparent=true,
      legend_show=true,
      shared_tooltip=false,
      fill=1,
      linewidth=2,
    )
    .addTarget(
      prometheus.target(
        'max by (environment, cluster, datacenter, rack, node)
        (jvm_memory_bytes_used{area="heap", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"}        )',
        legendFormat='Maximum heap size used for nodes in cluster: {{cluster}}',
      )
    )
    .addTarget(
      prometheus.target(
        'min by (environment, cluster, datacenter, rack, node)
        (jvm_memory_bytes_max{area="heap", environment="$environment", cluster="$cluster", datacenter=~"$datacenter", rack=~"$rack", node=~"$node"})',
        legendFormat='Minimum amount of heap memory available for nodes in cluster: {{cluster}}',
      )
    )
  )
)
