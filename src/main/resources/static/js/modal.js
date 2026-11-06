window.confirmModal = function (options) {
    const opt = typeof options === 'string' ? {message: options} : (options || {});

    return new Promise(function (resolve) {
        const dialog = document.createElement('dialog');
        dialog.className = 'confirm-modal';
        dialog.innerHTML =
            '<div class="body">' +
            '<div class="title"></div>' +
            '<div class="message"></div>' +
            '</div>' +
            '<div class="actions">' +
            '<button type="button" class="btn btn-line" data-cancel></button>' +
            '<button type="button" class="btn" data-ok></button>' +
            '</div>';

        dialog.querySelector('.title').textContent = opt.title || '확인';
        dialog.querySelector('.message').textContent = opt.message || '';
        const cancelBtn = dialog.querySelector('[data-cancel]');
        const okBtn = dialog.querySelector('[data-ok]');
        cancelBtn.textContent = opt.cancelText || '취소';
        okBtn.textContent = opt.confirmText || '확인';
        okBtn.classList.add(opt.danger ? 'btn-danger' : 'btn-primary');

        let result = false;
        okBtn.addEventListener('click', function () {
            result = true;
            dialog.close();
        });
        cancelBtn.addEventListener('click', function () {
            dialog.close();
        });
        dialog.addEventListener('click', function (e) {
            if (e.target === dialog) dialog.close();
        });
        dialog.addEventListener('close', function () {
            dialog.remove();
            resolve(result);
        });

        document.body.appendChild(dialog);
        dialog.showModal();
        cancelBtn.focus();
    });
};
