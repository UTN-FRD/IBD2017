(function (callback) {
  if (typeof define === 'function' && define.amd) {
    define(['core/AbstractManager'], callback);
  }
  else {
    callback();
  }
}(function () {

/**
 * @see http://wiki.apache.org/solr/SolJSON#JSON_specific_parameters
 * @class Manager
 * @augments AjaxSolr.AbstractManager
 */
AjaxSolr.Manager = AjaxSolr.AbstractManager.extend(
  /** @lends AjaxSolr.Manager.prototype */
  {
  executeRequest: function (servlet, string, handler, errorHandler, disableJsonp) {
    var self = this,
        options = {dataType: 'json'};
    string = string || this.store.string();
    handler = handler || function (data) {
      self.handleResponse(data);
    };
    errorHandler = errorHandler || function (jqXHR, textStatus, errorThrown) {
      self.handleError(textStatus + ', ' + errorThrown);
    };
    if (this.proxyUrl) {
      options.url = this.proxyUrl;
      options.data = {query: string};
      options.type = 'POST';
    }
    else {
      options.url = this.solrUrl + servlet + '?' + string + '&wt=json' + (disableJsonp ? '' : '&json.wrf=?');
    }

    //hacer llamada ajax para guardar la url del filtro que está en options.url
    //con su respetivo idUsuario, e idInvestigacion
    var params = {};
    params.url = options.url;
    params.idInvestigacion = idInvestigacion;
    params.idUsuario = idUsuario;

    $.ajax({
      url: //almacena query
      data: params,
      type: 'POST'
    }).done(function() {
      $(this).addClass('done');
    });

    //alert("Query llamada: "+options.url)

    jQuery.ajax(options).done(handler).fail(errorHandler);
  }
});

}));
