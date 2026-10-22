/**
 * 服务器端的核心配置
 */

/**
 * 服务代理地址
 */
const server_url='/api'; //nginx代理
/**
 * 文件服务器地址
 */
const file_url="/api"; // 使用代理 例如:/api 或者使用文件服务器地址 例如:http://192.168.110.206:8081

/**
 * 把文件地址解析为浏览器可访问地址
 * 新数据保存相对路径, 统一拼接文件代理前缀;
 * 历史数据中的绝对地址只取 /file/** 路径, 迁移服务器后仍可访问
 */
window.yaya_resolve_file_url = function(fileUrl) {
    const value = String(fileUrl == null ? '' : fileUrl).trim();
    if (!value) {
        return '';
    }
    const base = String(file_url || '').replace(/\/+$/, '');
    if (/^(data|blob):/i.test(value)) {
        return value;
    }
    if (/^(https?:)?\/\//i.test(value)) {
        try {
            const pathname = new URL(value, window.location.href).pathname;
            if (pathname.indexOf('/api/file/') === 0) {
                return base + pathname.substring('/api'.length);
            }
            if (pathname.indexOf('/file/') === 0) {
                return base + pathname;
            }
        } catch (ignored) {
        }
        return value;
    }
    if (base && value.indexOf(base + '/') === 0) {
        return value;
    }
    return base + '/' + value.replace(/^\/+/, '');
};

/**
 * 公告富文本白名单清洗(客户端兜底, 服务端落库和读取时也会清洗)
 */
window.yaya_sanitize_html = (function () {
    const allowedTags = new Set([
        'a', 'abbr', 'b', 'blockquote', 'br', 'caption', 'code', 'col', 'colgroup',
        'dd', 'div', 'dl', 'dt', 'em', 'figcaption', 'figure', 'h1', 'h2', 'h3',
        'h4', 'h5', 'h6', 'hr', 'i', 'img', 'li', 'ol', 'p', 'pre', 'q', 's',
        'small', 'source', 'span', 'strike', 'strong', 'sub', 'sup', 'table',
        'tbody', 'td', 'tfoot', 'th', 'thead', 'tr', 'u', 'ul', 'video'
    ]);
    const dropTags = new Set([
        'script', 'style', 'iframe', 'object', 'embed', 'form', 'input', 'button',
        'svg', 'math', 'template', 'noscript', 'link', 'meta', 'base', 'frame',
        'frameset', 'textarea', 'select', 'option', 'title'
    ]);
    const allowedAttrs = new Set([
        'class', 'style', 'href', 'src', 'alt', 'title', 'target', 'rel',
        'controls', 'poster', 'width', 'height', 'colspan', 'rowspan', 'scope',
        'type', 'start', 'reversed'
    ]);

    function isSafeUrl(value, allowContact) {
        const text = String(value == null ? '' : value).trim();
        if (!text) {
            return false;
        }
        // 浏览器解析URL时会忽略控制字符/空白, 这里先去干净再判断协议
        const normalized = text.replace(/[\u0000-\u0020]/g, '').toLowerCase();
        if (normalized.charAt(0) === '#' || normalized.charAt(0) === '/'
            || normalized.indexOf('./') === 0 || normalized.indexOf('../') === 0) {
            return true;
        }
        const scheme = normalized.match(/^([a-z][a-z0-9+.-]*):/i);
        if (!scheme) {
            return true;
        }
        const protocol = scheme[1].toLowerCase();
        return protocol === 'http' || protocol === 'https'
            || (allowContact && (protocol === 'mailto' || protocol === 'tel'));
    }

    return function (html) {
        const template = document.createElement('template');
        template.innerHTML = String(html == null ? '' : html);
        Array.from(template.content.querySelectorAll('*')).forEach(function (element) {
            const tag = element.tagName.toLowerCase();
            if (dropTags.has(tag)) {
                element.remove();
                return;
            }
            if (!allowedTags.has(tag)) {
                element.replaceWith.apply(element, Array.from(element.childNodes));
                return;
            }
            Array.from(element.attributes).forEach(function (attr) {
                const name = attr.name.toLowerCase();
                const value = String(attr.value || '').trim();
                if (!allowedAttrs.has(name) || name.indexOf('on') === 0) {
                    element.removeAttribute(attr.name);
                    return;
                }
                if ((name === 'href' || name === 'src' || name === 'poster')
                    && !isSafeUrl(value, name === 'href')) {
                    element.removeAttribute(attr.name);
                    return;
                }
                if (name === 'style'
                    && /expression\s*\(|javascript\s*:|vbscript\s*:|behavior\s*:|url\s*\(/i.test(value)) {
                    element.removeAttribute(attr.name);
                }
            });
            if (tag === 'a' && element.getAttribute('target') === '_blank') {
                element.setAttribute('rel', 'noopener noreferrer');
            }
        });
        return template.innerHTML;
    };
})();

/**
 * 浏览器本地存储对象
 */
const yaya = layui.data('yaya') || {};
/**
 * token令牌
 * 使用 let: 后端临期续签后会通过响应头更新, 不能保持页面加载时的静态值
 */
let token = yaya.token ? ("Bearer " + yaya.token) : '';
/**
 * 按钮权限
 */
const perms = Array.isArray(yaya.permCodes) ? yaya.permCodes : [];
/**
 * 用户信息
 */
const user = yaya.user || {};
/**
 * 当前登录的用户ID
 */
const userId = user.userId || '';
/**
 * 部门信息
 */
const dept = yaya.dept || {};
/**
 * 当前登录用户的部门ID
 */
const deptId = dept.deptId || '';

/**
 * 角色信息
 */
const roles = Array.isArray(yaya.roles) ? yaya.roles : [];

/**
 * 是否是超级管理员 true=是,false=否
 * isSuper 由登录接口返回; 同时兼容老会话中用角色编号判断
 */
const is_super = yaya.isSuper === true || roles.some(function (role) {
    return role && String(role.roleCode || '').toUpperCase() === 'ROOT';
});

/**
 * 转义HTML, 防止权限提示/按钮文本中的特殊字符破坏页面结构
 */
function yaya_permission_escape(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (char) {
        return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[char];
    });
}

/**
 * 判断当前用户是否拥有指定角色(角色编号大小写不敏感)
 */
window.hasRole = function(roleCode) {
    const expected = String(roleCode || '').toUpperCase();
    if (!expected) {
        return false;
    }
    return roles.some(function (role) {
        return role && String(role.roleCode || '').toUpperCase() === expected;
    });
};

/**
 * 判断当前用户是否拥有任意一个指定角色
 * @param {string|string[]} roleCodes 角色编号或角色编号数组
 */
window.hasAnyRole = function(roleCodes) {
    const list = Array.isArray(roleCodes)
        ? roleCodes
        : String(roleCodes || '').split(',');
    return list.some(function (roleCode) {
        return window.hasRole(roleCode);
    });
};

/**
 * 定义一个全局可见的权限判断函数
 * ROOT 不受按钮权限限制; 无权限码的动作视为不需要权限
 */
window.hasPermission = function(authCode) {
    if (!authCode) {
        return true;
    }
    if (is_super) {
        return true;
    }
    return perms.includes(String(authCode));
};

/**
 * 判断一个元素是否满足 data-perm / data-role-any 的权限要求
 */
function yaya_element_permitted(element) {
    if (!element) {
        return true;
    }
    if (element.hasAttribute('data-perm')) {
        return window.hasPermission(element.getAttribute('data-perm'));
    }
    if (element.hasAttribute('data-role-any')) {
        return window.hasAnyRole(element.getAttribute('data-role-any'));
    }
    return true;
}

/**
 * 统一处理静态按钮权限
 * 无权限时保持按钮可见, 但添加禁用样式和 disabled 属性
 * 说明: 页面自身因"数据加载中"设置的 disabled 不会被移除
 * @param {Element|string} [root] 可选, 默认为 document
 */
window.yaya_apply_permission_buttons = function(root) {
    if (!window.layui || !layui.$) {
        return;
    }
    const $ = layui.$;
    const $scope = root ? $(root) : $(document);
    const $buttons = $scope.find('[data-perm],[data-role-any]');
    if ($scope.is('[data-perm],[data-role-any]')) {
        $buttons.push($scope[0]);
    }
    $buttons.each(function () {
        const $button = $(this);
        if (yaya_element_permitted(this)) {
            return;
        }
        const authCode = $button.attr('data-perm');
        const roleCodes = $button.attr('data-role-any');
        $button
            .addClass('yaya-btn-disabled layui-btn-disabled')
            .attr({
                'disabled': 'disabled',
                'aria-disabled': 'true',
                'data-perm-blocked': '1',
                'title': '无权限：' + (authCode || roleCodes || '未配置权限')
            });
    });
};

/**
 * 捕获阶段兜底阻止无权限按钮的点击事件
 * 兼容非 button 元素, 以及页面脚本后来把 disabled 打开的情况
 */
function yaya_block_disallowed_event(event) {
    const target = event.target && event.target.closest
        ? event.target.closest('[data-perm],[data-role-any]')
        : null;
    if (target && !yaya_element_permitted(target)) {
        event.preventDefault();
        event.stopImmediatePropagation();
    }
}
document.addEventListener('click', yaya_block_disallowed_event, true);
// 禁用状态的上传区域同样阻止拖拽上传
['dragenter', 'dragover', 'drop'].forEach(function (eventName) {
    document.addEventListener(eventName, yaya_block_disallowed_event, true);
});

/**
 * 表单页面常在数据加载完成后移除 disabled
 * 监听 disabled 变化, 对无权限按钮立即恢复禁用状态
 */
(function () {
    if (!window.MutationObserver) {
        return;
    }
    let scheduled = false;
    const observer = new MutationObserver(function (mutations) {
        if (scheduled) {
            return;
        }
        const shouldReapply = mutations.some(function (mutation) {
            const target = mutation.target;
            return mutation.type === 'attributes'
                && target
                && (target.hasAttribute('data-perm') || target.hasAttribute('data-role-any'))
                && !target.hasAttribute('disabled')
                && !yaya_element_permitted(target);
        });
        if (!shouldReapply) {
            return;
        }
        scheduled = true;
        Promise.resolve().then(function () {
            window.yaya_apply_permission_buttons();
            scheduled = false;
        });
    });
    observer.observe(document.documentElement, {
        subtree: true,
        attributes: true,
        attributeFilter: ['disabled']
    });
})();

/**
 * 动态操作按钮渲染器
 * 使用 LayUI laytpl 模板引擎生成操作列, 无权限按钮仍然渲染但禁用
 * @param {Array} actions 操作定义: {text, event, perm, className, style, title}
 * @returns {string} 按钮HTML
 */
window.yaya_permission_actions = function(actions) {
    const actionList = (Array.isArray(actions) ? actions : []).filter(Boolean).map(function (action) {
        const allowed = action.allowed === undefined
            ? window.hasPermission(action.perm)
            : action.allowed === true;
        return {
            text: yaya_permission_escape(action.text),
            event: yaya_permission_escape(action.event),
            className: yaya_permission_escape(action.className || 'yaya-btn is-link-primary yaya-btn-small'),
            style: yaya_permission_escape(action.style),
            title: yaya_permission_escape(action.title || '无权限：' + (action.perm || '未配置权限')),
            allowed: allowed
        };
    });
    const actionTemplate = [
        '<span class="yaya-permission-actions">',
        '{{# layui.each(d.actions, function(index, action) { }}',
        '<button type="button" class="{{ action.className }}{{# if (!action.allowed) { }} yaya-btn-disabled layui-btn-disabled{{# } }}"',
        '{{# if (action.event) { }} lay-event="{{ action.event }}"{{# } }}',
        '{{# if (action.style) { }} style="{{ action.style }}"{{# } }}',
        '{{# if (!action.allowed) { }} disabled="disabled" aria-disabled="true" title="{{ action.title }}"{{# } }}>',
        '{{ action.text }}</button>',
        '{{# }); }}',
        '</span>'
    ].join('');

    if (layui.laytpl) {
        return layui.laytpl(actionTemplate).render({actions: actionList});
    }

    // laytpl 尚未加载时使用等价渲染, 避免表格操作列空白
    return '<span class="yaya-permission-actions">' + actionList.map(function (action) {
        return '<button type="button" class="' + action.className
            + (action.allowed ? '' : ' yaya-btn-disabled layui-btn-disabled') + '"'
            + (action.event ? ' lay-event="' + action.event + '"' : '')
            + (action.style ? ' style="' + action.style + '"' : '')
            + (action.allowed ? '' : ' disabled="disabled" aria-disabled="true" title="' + action.title + '"')
            + '>' + action.text + '</button>';
    }).join('') + '</span>';
};

/**
 * 计算前端页面根路径和登录页地址, 兼容部署在子路径的场景
 */
const yayaRootUrl = (function () {
    const currentScript = document.currentScript;
    if (!currentScript || !currentScript.src) {
        return new URL('/', window.location.href);
    }
    try {
        return new URL('../', currentScript.src);
    } catch (e) {
        return new URL('/', window.location.href);
    }
})();
const yayaLoginUrl = new URL('login.html', yayaRootUrl).href;
const yayaIsLoginPage = /\/login\.html$/i.test(window.location.pathname);

/**
 * 清除登录本地数据
 */
function yaya_clear_login_storage() {
    layui.data('yaya', null);
    token = '';
}

/**
 * 统一处理 401: 清理本地登录数据并跳转登录页
 * 在 iframe 中触发时由顶层窗口跳转, 避免只把弹窗内容换成登录页
 */
function yaya_handle_unauthorized() {
    if (yayaIsLoginPage) {
        yaya_clear_login_storage();
        return;
    }
    if (window.__yaya_auth_redirecting) {
        return;
    }
    try {
        const lastRedirectAt = Number(sessionStorage.getItem('yaya_auth_redirect_at') || 0);
        if (lastRedirectAt && Date.now() - lastRedirectAt < 2000) {
            return;
        }
        sessionStorage.setItem('yaya_auth_redirect_at', String(Date.now()));
    } catch (e) {
        // 忽略隐私模式/存储禁用等异常
    }
    window.__yaya_auth_redirecting = true;
    yaya_clear_login_storage();
    try {
        if (window !== window.top) {
            window.top.location.replace(yayaLoginUrl);
        } else {
            window.location.replace(yayaLoginUrl);
        }
    } catch (e) {
        window.location.replace(yayaLoginUrl);
    }
}

/**
 * 应用后端续签后的 token, 并通知顶层窗口同步(弹窗页面续签后主页面也能拿到新token)
 */
function yaya_apply_refreshed_token(newToken, broadcast) {
    if (!newToken) {
        return;
    }
    layui.data('yaya', {
        key: 'token',
        value: newToken
    });
    token = 'Bearer ' + newToken;
    if (broadcast && window !== window.top) {
        try {
            window.top.postMessage({
                type: 'yaya-token-refreshed',
                token: newToken
            }, window.location.origin);
        } catch (e) {
            // 跨域顶层窗口无法通信时忽略, 当前页面 token 已更新
        }
    }
}

/**
 * 统一包装 fetch 请求的续签响应头和 401 判断
 * 只克隆 JSON 响应体读取 code, 文件流响应保持原样
 */
(function () {
    if (typeof window.fetch !== 'function') {
        return;
    }
    const originalFetch = window.fetch;
    window.fetch = function () {
        return originalFetch.apply(this, arguments).then(function (response) {
            const refreshedToken = response.headers.get('Authorization-Refresh');
            if (refreshedToken) {
                yaya_apply_refreshed_token(refreshedToken, true);
            }
            const contentType = response.headers.get('content-type') || '';
            if (response.status === 401) {
                yaya_handle_unauthorized();
            } else if (contentType.indexOf('application/json') >= 0) {
                response.clone().json().then(function (body) {
                    if (body && Number(body.code) === 401) {
                        yaya_handle_unauthorized();
                    }
                }).catch(function () {
                    // 非JSON响应或响应体已失效时忽略
                });
            }
            return response;
        });
    };
})();

/**
 * 监听 token 临期续签响应头 + 统一登录失效处理
 */
layui.use(function () {
    const $ = layui.$;

    $(document).ajaxComplete(function (event, xhr) {
        const newToken = xhr.getResponseHeader('Authorization-Refresh');
        if (newToken) {
            yaya_apply_refreshed_token(newToken, true);
        }

        // HTTP 401(标准状态) 或统一响应体 code=401(当前后端约定)
        let response = xhr.responseJSON;
        if (!response && xhr.responseText) {
            try {
                response = JSON.parse(xhr.responseText);
            } catch (e) {
                response = null;
            }
        }
        if (xhr.status === 401 || (response && Number(response.code) === 401)) {
            yaya_handle_unauthorized();
        }
    });

    // 顶层页面接收 iframe 的 token 续签通知
    window.addEventListener('message', function (event) {
        if (event.origin !== window.location.origin || !event.data) {
            return;
        }
        if (event.data.type === 'yaya-token-refreshed') {
            yaya_apply_refreshed_token(event.data.token, false);
        } else if (event.data.type === 'yaya-unauthorized') {
            yaya_handle_unauthorized();
        }
    });

    // 其它标签页登录/退出/token续签后同步当前页面变量
    window.addEventListener('storage', function () {
        const stored = layui.data('yaya') || {};
        if (stored.token) {
            token = 'Bearer ' + stored.token;
        }
    });

    window.yaya_apply_permission_buttons();
});
