(function () {

    // ── Interceptor de CSRF ───────────────────────────────────────────────────
    // Injeta automaticamente X-XSRF-TOKEN em toda requisição de escrita (POST, PUT, PATCH, DELETE),
    // evitando 403 Forbidden do Spring Security em qualquer página que carregue este script.
    var _fetchOriginal = window.fetch;
    window.fetch = function (input, init) {
        init = init || {};
        var method = (init.method || 'GET').toUpperCase();
        if (method === 'POST' || method === 'PUT' || method === 'PATCH' || method === 'DELETE') {
            var match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
            var token = match ? decodeURIComponent(match[1]) : null;
            if (token) {
                init.headers = Object.assign({}, init.headers || {}, { 'X-XSRF-TOKEN': token });
                init.credentials = init.credentials || 'same-origin';
            }
        }
        return _fetchOriginal.call(this, input, init);
    };

    // ── Modal de notificação ──────────────────────────────────────────────────
    var ID = 'ntf-modal';

    var TIPOS = {
        danger:    { titulo: 'Erro',    bg: 'bg-danger',    text: 'text-white', btn: 'btn-danger'    },
        warning:   { titulo: 'Atenção', bg: 'bg-warning',   text: 'text-dark',  btn: 'btn-warning'   },
        success:   { titulo: 'Sucesso', bg: 'bg-success',   text: 'text-white', btn: 'btn-success'   },
        secondary: { titulo: 'Aviso',   bg: 'bg-secondary', text: 'text-white', btn: 'btn-secondary' },
        info:      { titulo: 'Info',    bg: 'bg-info',      text: 'text-dark',  btn: 'btn-info'      }
    };

    function criarModal() {
        if (document.getElementById(ID)) return;
        var wrapper = document.createElement('div');
        wrapper.innerHTML =
            '<div class="modal fade" id="' + ID + '" tabindex="-1">' +
              '<div class="modal-dialog modal-dialog-centered modal-sm">' +
                '<div class="modal-content border-0 shadow-lg rounded-3">' +
                  '<div class="modal-header py-2" id="' + ID + '-hdr">' +
                    '<span class="modal-title fw-semibold" id="' + ID + '-titulo"></span>' +
                    '<button type="button" id="' + ID + '-close" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>' +
                  '</div>' +
                  '<div class="modal-body py-3 text-center">' +
                    '<p class="mb-0 text-break" id="' + ID + '-msg"></p>' +
                  '</div>' +
                  '<div class="modal-footer py-2 justify-content-center border-0">' +
                    '<button type="button" class="btn btn-sm px-4" id="' + ID + '-ok" data-bs-dismiss="modal">OK</button>' +
                  '</div>' +
                '</div>' +
              '</div>' +
            '</div>';
        document.body.appendChild(wrapper.firstElementChild);
    }

    window.mostrarToast = function (mensagem, tipo) {
        tipo = tipo || 'info';
        var cfg = TIPOS[tipo] || TIPOS.info;

        criarModal();

        var hdr    = document.getElementById(ID + '-hdr');
        var titulo = document.getElementById(ID + '-titulo');
        var msg    = document.getElementById(ID + '-msg');
        var ok     = document.getElementById(ID + '-ok');
        var close  = document.getElementById(ID + '-close');

        hdr.className    = 'modal-header py-2 ' + cfg.bg;
        titulo.className = 'modal-title fw-semibold ' + cfg.text;
        titulo.textContent = cfg.titulo;
        msg.textContent  = mensagem;
        ok.className     = 'btn btn-sm px-4 ' + cfg.btn;
        close.className  = 'btn-close' + (cfg.text === 'text-dark' ? '' : ' btn-close-white');

        var modal = bootstrap.Modal.getOrCreateInstance(document.getElementById(ID));
        modal.show();

        if (tipo !== 'danger') {
            setTimeout(function () { try { modal.hide(); } catch (e) {} }, 4000);
        }
    };

})();
