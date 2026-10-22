/**
 * 菜单图标选择器
 * 基于 layui dropdown, 提供 bootstrap-icons 与 layui 常用图标
 */
(function () {
    var ICONS = [
        // bootstrap-icons
        'bi bi-gear-fill',
        'bi bi-people-fill',
        'bi bi-person-badge-fill',
        'bi bi-diagram-3-fill',
        'bi bi-list-nested',
        'bi bi-folder-fill',
        'bi bi-key-fill',
        'bi bi-megaphone-fill',
        'bi bi-tags-fill',
        'bi bi-chat-dots-fill',
        'bi bi-briefcase-fill',
        'bi bi-journal-code',
        'bi bi-box-arrow-in-right',
        'bi bi-hdd-stack-fill',
        'bi bi-speedometer2',
        'bi bi-house-fill',
        'bi bi-bar-chart-fill',
        'bi bi-file-earmark-text-fill',
        'bi bi-cloud-upload-fill',
        'bi bi-shield-lock-fill',
        'bi bi-bell-fill',
        'bi bi-calendar-event-fill',
        'bi bi-search',
        'bi bi-pencil-square',
        'bi bi-trash-fill',
        'bi bi-plus-circle-fill',
        'bi bi-eye-fill',
        'bi bi-download',
        'bi bi-upload',
        'bi bi-link-45deg',
        'bi bi-database-fill',
        'bi bi-cpu-fill',
        'bi bi-command',
        'bi bi-grid-fill',
        'bi bi-layers-fill',
        'bi bi-person-gear',
        'bi bi-box-seam-fill',
        'bi bi-truck',
        'bi bi-currency-dollar',
        'bi bi-cart-fill',
        'bi bi-star-fill',
        'bi bi-check-circle-fill',
        'bi bi-info-circle-fill',
        'bi bi-exclamation-triangle-fill',
        'bi bi-image-fill',
        'bi bi-map-fill',
        'bi bi-envelope-fill',
        'bi bi-telephone-fill',
        'bi bi-building-fill',
        'bi bi-clipboard-data-fill',
        // layui
        'layui-icon layui-icon-home',
        'layui-icon layui-icon-set',
        'layui-icon layui-icon-user',
        'layui-icon layui-icon-group',
        'layui-icon layui-icon-menu-fill',
        'layui-icon layui-icon-list',
        'layui-icon layui-icon-table',
        'layui-icon layui-icon-file',
        'layui-icon layui-icon-folder',
        'layui-icon layui-icon-edit',
        'layui-icon layui-icon-delete',
        'layui-icon layui-icon-add-1',
        'layui-icon layui-icon-search',
        'layui-icon layui-icon-refresh',
        'layui-icon layui-icon-upload-circle',
        'layui-icon layui-icon-password',
        'layui-icon layui-icon-notice',
        'layui-icon layui-icon-chart',
        'layui-icon layui-icon-console',
        'layui-icon layui-icon-log'
    ];

    window.yayaIconPicker = {
        /**
         * 渲染图标选择下拉
         *
         * @param dropdown       layui.dropdown 模块
         * @param inputSelector  图标输入框选择器
         * @param buttonSelector 选择按钮选择器
         */
        render: function (dropdown, inputSelector, buttonSelector) {
            var $ = layui.$;
            var $input = $(inputSelector);
            var $button = $(buttonSelector);
            if (!$input.length || !$button.length) {
                return;
            }

            var data = ICONS.map(function (icon) {
                return {
                    id: icon,
                    title: icon,
                    templet: function (item) {
                        return '<i class="' + item.id + '" title="' + item.id + '"></i>';
                    }
                };
            });

            dropdown.render({
                elem: $button[0],
                data: data,
                className: 'yaya-icon-dropdown',
                click: function (item) {
                    $input.val(item.id);
                    $input.trigger('input');
                }
            });
        }
    };
})();
