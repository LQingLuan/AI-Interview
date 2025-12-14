document.addEventListener('DOMContentLoaded', function() {
    const API_BASE_URL = (function() {
        if (window.location.hostname === 'localhost' && window.location.port === '63342') {
            // IDE预览模式
            return 'http://localhost:8080';
        } else if (window.location.hostname === 'localhost' && window.location.port === '3000') {
            // 前端开发服务器
            return 'http://localhost:8080';
        } else if (window.location.hostname === 'localhost' && window.location.port === '8080') {
            // 静态文件由Spring Boot提供
            return window.location.origin;
        } else {
            // 生产环境
            return window.location.origin;
        }
    })();

    console.log('当前API基础URL:', API_BASE_URL);

    // 从localStorage获取用户信息
    function getUserInfo() {
        const userInfo = localStorage.getItem('user_info');
        return userInfo ? JSON.parse(userInfo) : null;
    }

    // 保存用户信息到localStorage
    function saveUserInfo(userInfo) {
        localStorage.setItem('user_info', JSON.stringify(userInfo));
    }

    // 删除用户信息
    function removeUserInfo() {
        localStorage.removeItem('user_info');
    }

    // 检查用户是否已登录（通过Session）
async function checkSession() {
    try {
        console.log('检查Session状态...');

        // 如果是登出后的访问，不检查Session
        const urlParams = new URLSearchParams(window.location.search);
        const isLogout = urlParams.get('logout') === 'true';

        if (isLogout) {
            console.log('登出后访问，跳过Session检查');
            // 清除登出参数
            const newUrl = window.location.pathname;
            window.history.replaceState({}, document.title, newUrl);
            return false;
        }

        const response = await fetch(`${API_BASE_URL}/api/auth/session-check`, {
            method: 'GET',
            credentials: 'include'
        });

        console.log('Session检查响应状态:', response.status);

        if (!response.ok) {
            console.log('Session检查失败');
            removeUserInfo();
            return false;
        }

        const result = await response.json();
        console.log('Session检查结果:', result);

        if (result.success && result.data.isLoggedIn) {
            console.log('用户已登录');

            // 保存用户信息到localStorage
            const userInfo = {
                username: result.data.username,
                userId: result.data.userId,
                userType: result.data.userType,
                isLoggedIn: true
            };
            saveUserInfo(userInfo);

            // 如果是登录页面，则跳转到start.html
            if (window.location.pathname.includes('user.html')) {
                console.log('用户已登录，跳转到start.html');
                // 添加延迟，让用户看到登录成功消息
                setTimeout(() => {
                    window.location.href = "start.html";
                }, 1000);
            }
            return true;
        } else {
            // 用户未登录
            console.log('用户未登录');
            removeUserInfo();
            return false;
        }
    } catch (error) {
        console.error('检查Session状态失败:', error);
        removeUserInfo();
        return false;
    }
}

    // ============ 2. 初始化登录表单 ============
    function initLoginForm() {
        // 获取表单元素
        const loginForm = document.getElementById('login-form');
        const registerForm = document.getElementById('register-form');
        const loginTab = document.getElementById('login-tab');
        const registerTab = document.getElementById('register-tab');
        const switchToRegister = document.getElementById('switch-to-register');
        const switchToLogin = document.getElementById('switch-to-login');

        // 标签切换功能
        if (loginTab && registerTab) {
            loginTab.addEventListener('click', function() {
                loginTab.classList.add('active');
                registerTab.classList.remove('active');
                if (loginForm) loginForm.classList.add('active');
                if (registerForm) registerForm.classList.remove('active');
            });

            registerTab.addEventListener('click', function() {
                registerTab.classList.add('active');
                loginTab.classList.remove('active');
                if (registerForm) registerForm.classList.add('active');
                if (loginForm) loginForm.classList.remove('active');
            });
        }

        if (switchToRegister) {
            switchToRegister.addEventListener('click', function(e) {
                e.preventDefault();
                if (registerTab) registerTab.click();
            });
        }

        if (switchToLogin) {
            switchToLogin.addEventListener('click', function(e) {
                e.preventDefault();
                if (loginTab) loginTab.click();
            });
        }

        // ============ 登录表单提交 ============
        if (loginForm) {
            loginForm.addEventListener('submit', async function(e) {
                e.preventDefault();

                const username = document.getElementById('login-username').value.trim();
                const password = document.getElementById('login-password').value.trim();

                if (!username || !password) {
                    showMessage('请输入用户名和密码', 'error');
                    return;
                }

                // 禁用按钮并显示加载状态
                const submitBtn = loginForm.querySelector('button[type="submit"]');
                const originalText = submitBtn.innerHTML;
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 登录中...';

                try {
                    console.log('尝试登录:', username);
                    console.log('请求URL:', `${API_BASE_URL}/api/auth/login`);

                    const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Accept': 'application/json'
                        },
                        credentials: 'include', // 包含Cookie，用于Session
                        body: JSON.stringify({ username, password })
                    });

                    console.log('登录响应状态:', response.status, response.statusText);

                    // 获取响应文本
                    const responseText = await response.text();

                    // 检查响应内容是否为HTML
                    if (responseText.trim().startsWith('<!DOCTYPE') ||
                        responseText.trim().startsWith('<html')) {
                        console.error('服务器返回了HTML页面而不是JSON');
                        showMessage('登录失败：服务器返回了错误页面，请检查配置', 'error');
                        return;
                    }

                    // 尝试解析JSON
                    let result;
                    try {
                        result = JSON.parse(responseText);
                    } catch (jsonError) {
                        console.error('JSON解析失败:', jsonError);
                        console.error('响应内容:', responseText);
                        showMessage('登录失败：服务器返回了无效的响应格式', 'error');
                        return;
                    }

                    console.log('登录响应数据:', result);

                    if (result.success) {
                        console.log('登录成功');

                        // 保存用户信息到localStorage
                        const userInfo = {
                            username: result.data.username,
                            userId: result.data.userId,
                            userType: result.data.userType,
                            isLoggedIn: true
                        };
                        saveUserInfo(userInfo);

                        // 显示成功消息
                        showMessage('登录成功！正在跳转...', 'success');

                        // 延迟跳转
                        setTimeout(() => {
                            window.location.href = "start.html";
                        }, 500);
                    } else {
                        showMessage(result.message || '登录失败，请检查用户名和密码', 'error');
                    }
                } catch (error) {
                    console.error('登录请求失败:', error);
                    showMessage('登录失败，请检查网络连接: ' + error.message, 'error');
                } finally {
                    // 恢复按钮状态
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = originalText;
                }
            });
        }

        // ============ 注册表单提交 ============
        if (registerForm) {
            registerForm.addEventListener('submit', async function(e) {
                e.preventDefault();

                const username = document.getElementById('register-username').value.trim();
                const password = document.getElementById('register-password').value.trim();
                let email = document.getElementById('register-email').value.trim();
                let phone = document.getElementById('register-phone').value.trim();

                // 验证输入
                if (!username || !password) {
                    showMessage('用户名和密码不能为空', 'error');
                    return;
                }

                // 用户名验证（3-20个字符，字母数字下划线）
                if (!/^[a-zA-Z0-9_]{3,20}$/.test(username)) {
                    showMessage('用户名必须是3-20个字符，只能包含字母、数字和下划线', 'error');
                    return;
                }

                // 密码验证（6-20个字符）
                if (password.length < 6 || password.length > 20) {
                    showMessage('密码长度必须在6-20个字符之间', 'error');
                    return;
                }

                // 邮箱验证（可选）
                if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                    showMessage('邮箱格式不正确', 'error');
                    return;
                }

                // 手机号验证（可选）
                if (phone && !/^1[3-9]\d{9}$/.test(phone)) {
                    showMessage('手机号格式不正确', 'error');
                    return;
                }

                // 如果邮箱或手机号是空的，设置为null而不是空字符串
                if (email === '') email = null;
                if (phone === '') phone = null;

                // 禁用按钮并显示加载状态
                const submitBtn = registerForm.querySelector('button[type="submit"]');
                const originalText = submitBtn.innerHTML;
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 注册中...';

                try {
                    console.log('尝试注册:', username, '邮箱:', email, '手机:', phone);

                    const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        credentials: 'include', // 包含Cookie，用于Session
                        body: JSON.stringify({
                            username,
                            password,
                            email,
                            phone
                        })
                    });

                    console.log('注册响应状态:', response.status);

                    // 先检查响应状态
                    if (!response.ok) {
                        // 尝试读取错误信息
                        const errorText = await response.text();
                        console.error('注册失败，响应内容:', errorText);

                        // 尝试解析为JSON
                        try {
                            const errorResult = JSON.parse(errorText);
                            showMessage(errorResult.message || '注册失败', 'error');
                        } catch {
                            // 如果不是JSON，显示原始错误
                            showMessage(`注册失败: ${response.status} ${response.statusText}`, 'error');
                        }
                        return;
                    }

                    // 解析响应数据
                    const result = await response.json();
                    console.log('注册响应数据:', result);

                    if (result.success) {
                        console.log('注册成功');

                        // 重要：注册成功时不保存用户信息到localStorage，也不自动登录
                        // 只显示成功消息并切换到登录标签页
                        
                        // 显示成功消息
                        showMessage('注册成功！请使用您的账号密码登录', 'success');
                        
                        // 清空注册表单（可选）
                        registerForm.reset();
                        
                        // 切换到登录标签页
                        if (loginTab) {
                            loginTab.click();
                        }
                        
                        // 自动填充用户名到登录表单
                        const loginUsernameInput = document.getElementById('login-username');
                        if (loginUsernameInput) {
                            loginUsernameInput.value = username;
                        }
                        
                        // 自动聚焦到密码输入框
                        const loginPasswordInput = document.getElementById('login-password');
                        if (loginPasswordInput) {
                            loginPasswordInput.focus();
                        }
                    } else {
                        showMessage(result.message || '注册失败', 'error');
                    }
                } catch (error) {
                    console.error('注册请求失败:', error);
                    showMessage('注册失败，请检查网络连接: ' + error.message, 'error');
                } finally {
                    // 恢复按钮状态
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = originalText;
                }
            });
        }

        // ============ 检查用户名可用性 ============
        const checkUsernameBtn = document.getElementById('check-username-btn');
        if (checkUsernameBtn) {
            checkUsernameBtn.addEventListener('click', async function() {
                const username = document.getElementById('register-username').value.trim();
                if (!username) {
                    showMessage('请输入用户名', 'warning');
                    return;
                }

                try {
                    const response = await fetch(`${API_BASE_URL}/api/auth/check-username?username=${encodeURIComponent(username)}`, {
                        method: 'GET',
                        credentials: 'include'
                    });

                    const result = await response.json();
                    if (result.success) {
                        const isAvailable = result.data.available;
                        showMessage(isAvailable ? '用户名可用' : '用户名已被使用',
                                   isAvailable ? 'success' : 'warning');
                    } else {
                        showMessage('检查失败: ' + result.message, 'error');
                    }
                } catch (error) {
                    console.error('检查用户名失败:', error);
                    showMessage('检查失败，请重试', 'error');
                }
            });
        }

        // ============ 检查邮箱可用性 ============
        const checkEmailBtn = document.getElementById('check-email-btn');
        if (checkEmailBtn) {
            checkEmailBtn.addEventListener('click', async function() {
                const email = document.getElementById('register-email').value.trim();
                if (!email) {
                    showMessage('请输入邮箱', 'warning');
                    return;
                }

                // 邮箱格式验证
                if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                    showMessage('邮箱格式不正确', 'warning');
                    return;
                }

                try {
                    const response = await fetch(`${API_BASE_URL}/api/auth/check-email?email=${encodeURIComponent(email)}`, {
                        method: 'GET',
                        credentials: 'include'
                    });

                    const result = await response.json();
                    if (result.success) {
                        const isAvailable = result.data.available;
                        showMessage(isAvailable ? '邮箱可用' : '邮箱已被注册',
                                   isAvailable ? 'success' : 'warning');
                    } else {
                        showMessage('检查失败: ' + result.message, 'error');
                    }
                } catch (error) {
                    console.error('检查邮箱失败:', error);
                    showMessage('检查失败，请重试', 'error');
                }
            });
        }

        // ============ 显示当前用户信息（如果存在） ============
        const currentUserInfo = getUserInfo();
        if (currentUserInfo && currentUserInfo.username) {
            const welcomeMessage = document.getElementById('welcome-message');
            if (welcomeMessage) {
                welcomeMessage.textContent = `欢迎回来，${currentUserInfo.username}`;
                welcomeMessage.classList.remove('hidden');
            }
        }
    }

    // ============ 工具函数：显示消息 ============
    function showMessage(message, type = 'info') {
        // 移除现有的消息框
        const existingMessage = document.getElementById('custom-message');
        if (existingMessage) {
            existingMessage.remove();
        }

        // 创建消息框
        const messageDiv = document.createElement('div');
        messageDiv.id = 'custom-message';
        messageDiv.className = `message-box ${type}`;

        // 设置图标
        let icon = '';
        switch(type) {
            case 'success':
                icon = '<i class="fas fa-check-circle"></i>';
                break;
            case 'error':
                icon = '<i class="fas fa-exclamation-circle"></i>';
                break;
            case 'warning':
                icon = '<i class="fas fa-exclamation-triangle"></i>';
                break;
            default:
                icon = '<i class="fas fa-info-circle"></i>';
        }

        messageDiv.innerHTML = `
            ${icon}
            <span>${message}</span>
            <button class="message-close" onclick="this.parentElement.remove()">
                <i class="fas fa-times"></i>
            </button>
        `;

        // 添加到页面
        const container = document.querySelector('.container') || document.body;
        container.insertBefore(messageDiv, container.firstChild);

        // 3秒后自动消失
        setTimeout(() => {
            if (messageDiv.parentNode) {
                messageDiv.remove();
            }
        }, 3000);

        // 添加CSS样式
        if (!document.getElementById('message-styles')) {
            const style = document.createElement('style');
            style.id = 'message-styles';
            style.textContent = `
                .message-box {
                    position: relative;
                    padding: 12px 40px 12px 50px;
                    margin: 0 auto 20px;
                    border-radius: 8px;
                    font-size: 14px;
                    max-width: 600px;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                    animation: slideIn 0.3s ease;
                }
                .message-box.success {
                    background-color: #d1fae5;
                    color: #065f46;
                    border: 1px solid #a7f3d0;
                }
                .message-box.error {
                    background-color: #fee2e2;
                    color: #991b1b;
                    border: 1px solid #fecaca;
                }
                .message-box.warning {
                    background-color: #fef3c7;
                    color: #92400e;
                    border: 1px solid #fde68a;
                }
                .message-box.info {
                    background-color: #dbeafe;
                    color: #1e40af;
                    border: 1px solid #bfdbfe;
                }
                .message-box i.fa-check-circle {
                    color: #10b981;
                }
                .message-box i.fa-exclamation-circle {
                    color: #ef4444;
                }
                .message-box i.fa-exclamation-triangle {
                    color: #f59e0b;
                }
                .message-box i.fa-info-circle {
                    color: #3b82f6;
                }
                .message-box > i:first-child {
                    position: absolute;
                    left: 18px;
                    top: 13px;
                    font-size: 18px;
                }
                .message-close {
                    position: absolute;
                    right: 12px;
                    top: 50%;
                    transform: translateY(-50%);
                    background: none;
                    border: none;
                    cursor: pointer;
                    color: inherit;
                    opacity: 0.7;
                    padding: 4px;
                    font-size: 14px;
                }
                .message-close:hover {
                    opacity: 1;
                }
                @keyframes slideIn {
                    from {
                        transform: translateY(-20px);
                        opacity: 0;
                    }
                    to {
                        transform: translateY(0);
                        opacity: 1;
                    }
                }
            `;
            document.head.appendChild(style);
        }
    }

    // ============ 页面初始化逻辑 ============
    // 如果当前页面是登录页面，初始化登录表单
    if (window.location.pathname.includes('user.html')) {
        console.log('登录页面，初始化登录表单');
        initLoginForm();

        // 检查Session状态，如果已登录则跳转
        checkSession();
    } else {
        // 其他页面，检查Session状态
        console.log('非登录页面，检查Session状态');
        checkSession();
    }

    // 设置全局 AJAX 请求拦截器
        setupAjaxInterceptor();

});

// 全局 AJAX 请求拦截器
function setupAjaxInterceptor() {
    // 保存原始的 XMLHttpRequest.open 方法
    const originalXHROpen = XMLHttpRequest.prototype.open;
    const originalFetch = window.fetch;

    // 重写 XMLHttpRequest.open 方法
    XMLHttpRequest.prototype.open = function(...args) {
        this.addEventListener('load', function() {
            if (this.status === 401) {
                console.log('收到 401 响应，用户未登录');
                handleUnauthorized();
            }
        });
        return originalXHROpen.apply(this, args);
    };

    // 重写 fetch 方法
    window.fetch = async function(...args) {
        try {
            const response = await originalFetch.apply(this, args);

            if (response.status === 401) {
                console.log('fetch 收到 401 响应，用户未登录');
                handleUnauthorized();
                return response;
            }

            return response;
        } catch (error) {
            console.error('请求失败:', error);
            throw error;
        }
    };

    // 处理未授权
    function handleUnauthorized() {
        // 清除本地存储
        localStorage.removeItem('user_info');

        // 如果不在登录页面，则跳转到登录页面
        if (!window.location.pathname.includes('user.html')) {
            console.log('跳转到登录页面');
            window.location.href = "user.html";
        }
    }
}


// 全局函数，供HTML内联事件调用
window.logoutUser = async function() {
    try {
        // 使用当前页面的API_BASE_URL
        const currentUrl = window.location.origin;
        const response = await fetch(`${currentUrl}/api/auth/logout`, {
            method: 'POST',
            credentials: 'include'
        });

        if (response.ok) {
            console.log('退出登录成功');
        }
    } catch (error) {
        console.error('退出登录失败:', error);
    } finally {
        // 清除本地存储的用户信息
        localStorage.removeItem('user_info');
        // 跳转到登录页面
        window.location.href = "user.html";
    }
};
