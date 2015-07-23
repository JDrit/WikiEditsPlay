$(function () {
    Highcharts.setOptions({
        global: {
            useUTC: false
        }
    });
    $.getJSON('/api/channel_edits/' + subdomain + '?callback=?', function (data) {

        // Create the chart
        $('#graph-container').highcharts('StockChart', {
            chart: {
                events: {
                    load: function() {
                        var series = this.series[0];
                        console.log(series);
                        setInterval(function() {
                            $.getJSON('/api/channel_edits_update/' + subdomain + '?callback=?', function(newData) {
                                series.addPoint(newData, true, true);
                            })
                        }, 5000);
                    }
                }
            },
            rangeSelector: {
                buttons: [{
                    type: 'minute',
                    count: 5,
                    text: '5m',
                },{
                    type: 'hour',
                    count: 1,
                    text: '1h'
                }, {
                    type: 'hour',
                    count: 24,
                    text: '1d'
                }, {
                    type: 'day',
                    count: 3,
                    text: '3d'
                },{
                    type: 'all',
                    text: 'All'
                }],
            },
            yAxis: {
                title: {
                    text: 'Page Edits'
                }
            },
            title: {
                text: 'Number of Pages Being Edited per Hour'
            },
            subtitle: {
                text: 'subtitle'
            },
            tooltip: {
                pointFormat: "{point.y:.0f} edits/hr"
            },
            series: [{
                name: 'Page edits',
                type: 'spline',
                data: data
            }]
        });
    });
});
