(function() {
    console.log('页面初始化开始...');

    // 等待 DOM 加载完成
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeApp);
    } else {
        initializeApp();
    }

    function initializeApp() {
        console.log('开始初始化应用程序...');

        // 初始化侧栏
        if (typeof initPage === 'function') {
            initPage();
            console.log('侧栏初始化完成');
        }

        // 初始化其他模块
        if (typeof initInterviewApp === 'function') {
            initInterviewApp();
            console.log('面试模块初始化完成');
        }

        console.log('应用程序初始化完成');
    }
})();