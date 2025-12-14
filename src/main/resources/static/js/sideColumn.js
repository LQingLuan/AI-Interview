function initSidebarNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    const pages = document.querySelectorAll('.page');
    const pageTitle = document.getElementById('pageTitle');
    const pageSubtitle = document.getElementById('pageSubtitle');

    // 页面标题映射
    const pageTitles = {
        'startInterview': {
            title: '开始面试',
            subtitle: '选择职业方向和难度，开始您的智能面试'
        },
        'userCenter': {
            title: '用户中心',
            subtitle: '查看和管理您的个人信息'
        },
        'interviewHistory': {
            title: '面试历史',
            subtitle: '查看您的历史面试记录和成绩'
        },
        'settings': {
            title: '系统设置',
            subtitle: '个性化设置您的面试体验'
        }
    };

    // 导航点击事件
    navItems.forEach(item => {
        item.addEventListener('click', function() {
            const pageId = this.getAttribute('data-page');

            // 更新活跃导航项
            navItems.forEach(nav => nav.classList.remove('active'));
            this.classList.add('active');

            // 更新活跃页面
            pages.forEach(page => {
                page.classList.remove('active');
                if (page.id === `${pageId}Page`) {
                    page.classList.add('active');
                }
            });

            // 更新页面标题
            if (pageTitles[pageId]) {
                pageTitle.textContent = pageTitles[pageId].title;
                pageSubtitle.textContent = pageTitles[pageId].subtitle;
            }
        });
    });
}

// ============ 用户信息显示功能 ============
function displayUserInfo() {
    const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}');

    if (userInfo.username) {
        // 更新侧边栏用户信息
        document.getElementById('sidebarUsername').textContent = userInfo.username;
        document.getElementById('sidebarUserType').textContent = userInfo.userType || '普通用户';

        // 更新顶部用户指示器
        document.getElementById('currentUsername').textContent = userInfo.username;

        // 更新用户中心页面信息
        document.getElementById('userName').textContent = userInfo.username;
        document.getElementById('userId').textContent = userInfo.userId || '未获取';
        document.getElementById('userType').textContent = userInfo.userType || '普通用户';

        // 模拟一些数据（实际应从后端获取）
        document.getElementById('interviewCount').textContent = '3';
        document.getElementById('totalInterviews').textContent = '3';
        document.getElementById('avgScore').textContent = '7.5';
        document.getElementById('bestScore').textContent = '9.0';
        document.getElementById('improvement').textContent = '15%';
    }
}

// ============ 初始化用户中心功能 ============
function initUserCenter() {
    // 编辑资料按钮
    const editProfileBtn = document.getElementById('editProfileBtn');
    if (editProfileBtn) {
        editProfileBtn.addEventListener('click', function() {
            alert('编辑资料功能正在开发中...');
        });
    }

    // 修改密码按钮
    const changePasswordBtn = document.getElementById('changePasswordBtn');
    if (changePasswordBtn) {
        changePasswordBtn.addEventListener('click', function() {
            alert('修改密码功能正在开发中...');
        });
    }
}

// ============ 初始化面试历史功能 ============
function initInterviewHistory() {
    // 模拟历史数据
    const historyData = [
        {
            id: 1,
            position: '前端开发工程师',
            difficulty: '中级',
            date: '2024-01-15',
            score: '8.5/10'
        },
        {
            id: 2,
            position: '软件开发工程师',
            difficulty: '初级',
            date: '2024-01-10',
            score: '7.0/10'
        },
        {
            id: 3,
            position: '数据科学家',
            difficulty: '高级',
            date: '2024-01-05',
            score: '9.0/10'
        }
    ];

    const historyList = document.getElementById('historyList');
    const historyLoading = document.getElementById('historyLoading');

    // 模拟加载延迟
    setTimeout(() => {
        if (historyLoading) {
            historyLoading.style.display = 'none';
        }

        if (historyList) {
            historyData.forEach(item => {
                const historyItem = document.createElement('div');
                historyItem.className = 'history-item';
                historyItem.innerHTML = `
                    <div class="history-info">
                        <h3>${item.position}</h3>
                        <div class="history-meta">
                            <span><i class="fas fa-chart-line"></i> ${item.difficulty}</span>
                            <span><i class="fas fa-calendar-alt"></i> ${item.date}</span>
                            <span><i class="fas fa-clock"></i> 25分钟</span>
                        </div>
                    </div>
                    <div class="history-score">${item.score}</div>
                `;
                historyList.appendChild(historyItem);
            });

            // 如果没有历史记录
            if (historyData.length === 0) {
                historyList.innerHTML = `
                    <div class="no-history">
                        <i class="fas fa-history" style="font-size: 3rem; color: rgba(255,255,255,0.3); margin-bottom: 15px;"></i>
                        <h3>暂无面试历史</h3>
                        <p>开始您的第一次面试吧！</p>
                    </div>
                `;
            }
        }
    }, 1000);
}

// ============ 初始化系统设置功能 ============
function initSettings() {
    // 主题选择
    const themeSelect = document.getElementById('themeSelect');
    if (themeSelect) {
        const savedTheme = localStorage.getItem('appTheme') || 'default';
        themeSelect.value = savedTheme;

        themeSelect.addEventListener('change', function() {
            localStorage.setItem('appTheme', this.value);
            alert(`主题已切换为: ${this.options[this.selectedIndex].text}`);
        });
    }

    // 语音速度滑块
    const voiceSpeed = document.getElementById('voiceSpeed');
    const speedValue = document.getElementById('speedValue');
    if (voiceSpeed && speedValue) {
        const savedSpeed = localStorage.getItem('voiceSpeed') || 1;
        voiceSpeed.value = savedSpeed;
        speedValue.textContent = `${parseFloat(savedSpeed).toFixed(1)}x`;

        voiceSpeed.addEventListener('input', function() {
            speedValue.textContent = `${parseFloat(this.value).toFixed(1)}x`;
            localStorage.setItem('voiceSpeed', this.value);
        });
    }

    // 复选框设置
    const autoRecord = document.getElementById('autoRecord');
    const showTips = document.getElementById('showTips');
    const saveHistory = document.getElementById('saveHistory');

    // 加载保存的设置
    if (autoRecord) autoRecord.checked = localStorage.getItem('autoRecord') !== 'false';
    if (showTips) showTips.checked = localStorage.getItem('showTips') !== 'false';
    if (saveHistory) saveHistory.checked = localStorage.getItem('saveHistory') !== 'false';

    // 保存设置按钮
    const saveSettingsBtn = document.getElementById('saveSettingsBtn');
    if (saveSettingsBtn) {
        saveSettingsBtn.addEventListener('click', function() {
            // 保存所有设置
            if (autoRecord) localStorage.setItem('autoRecord', autoRecord.checked);
            if (showTips) localStorage.setItem('showTips', showTips.checked);
            if (saveHistory) localStorage.setItem('saveHistory', saveHistory.checked);

            showMessage('设置已保存成功！', 'success');
        });
    }

    // 恢复默认按钮
    const resetSettingsBtn = document.getElementById('resetSettingsBtn');
    if (resetSettingsBtn) {
        resetSettingsBtn.addEventListener('click', function() {
            if (confirm('确定要恢复默认设置吗？')) {
                localStorage.removeItem('appTheme');
                localStorage.removeItem('voiceSpeed');
                localStorage.removeItem('autoRecord');
                localStorage.removeItem('showTips');
                localStorage.removeItem('saveHistory');

                if (themeSelect) themeSelect.value = 'default';
                if (voiceSpeed) {
                    voiceSpeed.value = 1;
                    speedValue.textContent = '1.0x';
                }
                if (autoRecord) autoRecord.checked = true;
                if (showTips) showTips.checked = true;
                if (saveHistory) saveHistory.checked = true;

                showMessage('设置已恢复为默认值！', 'success');
            }
        });
    }
}

// ============ 显示消息函数 ============
function showMessage(message, type) {
    // 创建消息元素
    const messageEl = document.createElement('div');
    messageEl.className = `message ${type}`;
    messageEl.textContent = message;
    messageEl.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 12px 20px;
        background: ${type === 'success' ? '#4CAF50' : '#FF9800'};
        color: white;
        border-radius: 4px;
        z-index: 1000;
        animation: slideIn 0.3s ease;
    `;
    
    document.body.appendChild(messageEl);
    
    // 3秒后移除
    setTimeout(() => {
        messageEl.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => messageEl.remove(), 300);
    }, 3000);
}

// 添加CSS动画
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// ============ 退出登录功能 ============
function logoutUser() {
    if (confirm('确定要退出登录吗？')) {
        try {
            // 1. 先清除本地数据
            localStorage.removeItem('user_info');
            sessionStorage.removeItem('user_info');

            // 2. 调用服务器端登出接口
            const response = await fetch(`${API_BASE_URL}/api/auth/logout`, {
                method: 'POST',
                credentials: 'include' // 包含Cookie
            });

            if (response.ok) {
                console.log('服务器端登出成功');
            } else {
                console.warn('服务器端登出失败，但继续本地登出');
            }

        } catch (error) {
            console.error('登出请求出错:', error);
        } finally {
            // 3. 无论如何都跳转到登录页面
            // 使用 replace 而不是 href，防止后退按钮返回
            window.location.replace('user.html?logout=true');
        }
    }
}

// ============ 页面初始化 ============
function initPage() {
    // 初始化侧边栏导航
    initSidebarNavigation();

    // 显示用户信息
    displayUserInfo();

    // 初始化用户中心
    initUserCenter();

    // 初始化面试历史
    initInterviewHistory();

    // 初始化系统设置
    initSettings();
}

document.addEventListener('DOMContentLoaded', function() {
    // 等待 DOM 完全加载后初始化侧栏
    setTimeout(() => {
        try {
            initPage();
            console.log('侧栏初始化完成');
        } catch (error) {
            console.error('侧栏初始化失败:', error);
        }
    }, 100);
});