document.addEventListener('DOMContentLoaded', async function() {
    let config = {
        appId: '',
        apiKey: '',
        apiSecret: '',
        apiUrl: 'wss://iat-api.xfyun.cn/v2/iat'
    };

    // DOM元素引用
    const startRecordBtn = document.getElementById('startRecordBtn');
    const stopRecordBtn = document.getElementById('stopRecordBtn');
    const statusEl = document.getElementById('status');
    const recognitionResult = document.getElementById('recognitionResult');
    const visualizer = document.getElementById('visualizer');
    const interviewPage = document.getElementById('interviewPage');
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
    const feedbackPage = document.getElementById('feedbackPage');
    const startInterviewBtn = document.getElementById('startInterviewBtn');
    const submitAnswerBtn = document.getElementById('submitAnswerBtn');
    const newInterviewBtn = document.getElementById('newInterviewBtn');
    const downloadReportBtn = document.getElementById('downloadReportBtn');
    const questionSection = document.getElementById('questionSection');
    const questionContent = document.getElementById('questionContent');
    const recordingIndicator = document.getElementById('recordingIndicator');
    const feedbackQuestion = document.getElementById('feedbackQuestion');
    const userAnswer = document.getElementById('userAnswer');
    const overallFeedback = document.getElementById('overallFeedback');
    const improvementSuggestions = document.getElementById('improvementSuggestions');
    const technicalFeedback = document.getElementById('technicalFeedback');
    const communicationFeedback = document.getElementById('communicationFeedback');
    const scoreProgress = document.getElementById('scoreProgress');
    const scoreValue = document.getElementById('scoreValue');

    // ============ 新增DOM元素引用 ============
    const pauseRecordBtn = document.getElementById('pauseRecordBtn');
    const timeRemainingEl = document.createElement('div');
    timeRemainingEl.id = 'timeRemaining';
    timeRemainingEl.style = 'margin-top: 15px; font-weight: 500; color: #4361ee;';
    if (recordingIndicator) {
        recordingIndicator.parentNode.insertBefore(timeRemainingEl, recordingIndicator.nextSibling);
    }

    // 状态变量
    let isRecording = false;
    let isPaused = false; // 新增：暂停状态
    let audioContext = null;
    let analyser = null;
    let microphone = null;
    let audioProcessor = null;
    let websocket = null;
    let audioQueue = [];
    let sendInterval = null;
    let audioSamplesQueue = [];
    let currentTranscript = '';
    let mediaStream = null;
    let currentSessionId = null;
    let radarChart = null;
    let finalRecognitionResult = "";
    let isFinalResultReceived = false;
    let currentQuestion = ""; // 存储当前问题

    let currentInterviewId = null;  // 新增：当前面试的数据库ID

    // 新增：计时器相关变量
    let recordingStartTime = 0;
    let timeRemainingInterval = null;
    const MAX_THINKING_TIME = 10000; // 10秒思考时间

    // ============ 新增：思考时间计时器 ============
    function startThinkingTimer() {
        recordingStartTime = Date.now();
        clearInterval(timeRemainingInterval);

        timeRemainingInterval = setInterval(() => {
            if (!isRecording) return;

            const elapsed = Date.now() - recordingStartTime;
            const remaining = Math.max(0, MAX_THINKING_TIME - elapsed);
            const seconds = Math.ceil(remaining / 1000);

            if (timeRemainingEl) {
                timeRemainingEl.textContent = `思考时间剩余: ${seconds}秒`;

                // 根据剩余时间改变颜色
                if (seconds <= 3) {
                    timeRemainingEl.style.color = '#ef4444'; // 红色
                } else if (seconds <= 6) {
                    timeRemainingEl.style.color = '#f59e0b'; // 橙色
                } else {
                    timeRemainingEl.style.color = '#4361ee'; // 蓝色
                }
            }

            if (remaining <= 0) {
                clearInterval(timeRemainingInterval);
                if (timeRemainingEl) {
                    timeRemainingEl.textContent = '思考时间结束，请继续回答';
                    timeRemainingEl.style.color = '#ef4444';
                }
            }
        }, 1000);
    }

    // ============ 新增：暂停录音功能 ============
    function pauseRecording() {
        if (!isRecording || isPaused) return;

        isPaused = true;
        console.log("录音已暂停");

        // 停止音频处理
        if (audioProcessor) {
            audioProcessor.disconnect();
        }

        // 停止发送数据
        if (sendInterval) {
            clearInterval(sendInterval);
            sendInterval = null;
        }

        // 禁用麦克风
        if (mediaStream) {
            mediaStream.getTracks().forEach(track => track.enabled = false);
        }

        // 更新状态
        if (statusEl) statusEl.textContent = '录音已暂停';
        if (pauseRecordBtn) {
            pauseRecordBtn.innerHTML = '<i class="fas fa-play"></i> 继续录音';
            pauseRecordBtn.classList.remove('btn-warning');
            pauseRecordBtn.classList.add('btn-success');
        }
        if (timeRemainingEl) timeRemainingEl.textContent = '录音已暂停';
    }

    // ============ 新增：继续录音功能 ============
    function resumeRecording() {
        if (!isRecording || !isPaused) return;

        isPaused = false;
        console.log("录音已继续");

        // 重新连接音频处理
        if (audioProcessor) {
            microphone.connect(analyser);
            analyser.connect(audioProcessor);
            audioProcessor.connect(audioContext.destination);
        }

        // 重新启用麦克风
        if (mediaStream) {
            mediaStream.getTracks().forEach(track => track.enabled = true);
        }

        // 重启发送数据的定时器
        sendInterval = setInterval(sendAudioData, 40);

        // 更新状态
        if (statusEl) statusEl.textContent = '录音中...';
        if (pauseRecordBtn) {
            pauseRecordBtn.innerHTML = '<i class="fas fa-pause"></i> 暂停录音';
            pauseRecordBtn.classList.remove('btn-success');
            pauseRecordBtn.classList.add('btn-warning');
        }

        // 重启思考时间计时器
        startThinkingTimer();
    }

    // ============ 新增：发送音频数据的函数 ============
    function sendAudioData() {
        if (!websocket || websocket.readyState !== WebSocket.OPEN || audioQueue.length === 0) return;

        // 合并缓冲区中的所有数据
        const combinedData = new Int16Array(audioQueue.reduce((acc, buf) => acc + buf.length, 0));
        let offset = 0;

        for (const buf of audioQueue) {
            combinedData.set(buf, offset);
            offset += buf.length;
        }

        // 清空缓冲区
        audioQueue = [];

        // 转换为Base64字符串
        const base64Audio = arrayBufferToBase64(combinedData.buffer);

        // 分块发送 (每块不超过1280字节)
        const CHUNK_SIZE = 1280;
        for (let i = 0; i < base64Audio.length; i += CHUNK_SIZE) {
            const chunk = base64Audio.substring(i, i + CHUNK_SIZE);

            // 发送音频数据
            const audioData = {
                data: {
                    status: 1,
                    format: 'audio/L16;rate=16000',
                    encoding: 'raw',
                    audio: chunk
                }
            };

            // 检查WebSocket状态
            if (websocket && websocket.readyState === WebSocket.OPEN) {
                websocket.send(JSON.stringify(audioData));
            }
        }
    }

    // ============ 新增：从后端获取配置 ============
    async function fetchConfigFromBackend() {
        try {
            const response = await fetch(`${API_BASE_URL}/api/config/iflytek`);
            if (!response.ok) {
                throw new Error('获取配置失败: ' + response.status);
            }
            const configData = await response.json();
            console.log('从后端获取的配置:', configData);

            // 更新配置对象
            config = {
                appId: configData.appId,
                apiKey: configData.apiKey,
                apiSecret: configData.apiSecret,
                apiUrl: configData.apiUrl || 'wss://iat-api.xfyun.cn/v2/iat' // 默认值
            };

            return true;
        } catch (error) {
            console.error('获取配置失败:', error);
            if (statusEl) statusEl.textContent = `配置错误: ${error.message}`;
            return false;
        }
    }

    // ============ 整合的录音方法 ============
    // 生成音频可视化条
    function createVisualizerBars() {
        if (!visualizer) return;
        visualizer.innerHTML = '';
        for (let i = 0; i < 64; i++) {
            const bar = document.createElement('div');
            bar.className = 'bar';
            bar.style.height = '2px';
            visualizer.appendChild(bar);
        }
    }

    // 更新可视化效果
    function updateVisualization() {
        if (!analyser || !visualizer) return;

        const bufferLength = analyser.frequencyBinCount;
        const dataArray = new Uint8Array(bufferLength);
        analyser.getByteFrequencyData(dataArray);

        const bars = visualizer.querySelectorAll('.bar');
        if (!bars.length) return;

        for (let i = 0; i < bars.length; i++) {
            const value = dataArray[i] / 255;
            const height = value * 100 + 2;
            bars[i].style.height = `${height}px`;

            // 根据声音强度设置颜色
            const red = Math.min(255, 150 + value * 105);
            const green = Math.max(0, 100 - value * 100);
            const blue = Math.max(0, 50 - value * 50);
            bars[i].style.background = `rgb(${red}, ${green}, ${blue})`;
        }
    }

    // 优化后的Base64转换函数
    function arrayBufferToBase64(buffer) {
        return btoa(String.fromCharCode(...new Uint8Array(buffer)));
    }

    // 生成WebSocket请求URL（带签名）
    function getWebSocketUrl() {
        return new Promise((resolve, reject) => {
            if (!config.apiKey || !config.apiSecret) {
                reject('API密钥未配置');
                return;
            }

            // 生成RFC1123格式的日期
            const date = new Date().toUTCString();

            // 拼接签名字符串
            const signatureOrigin = `host: iat-api.xfyun.cn\ndate: ${date}\nGET /v2/iat HTTP/1.1`;

            try {
                // 使用Web Crypto API进行HMAC-SHA256加密
                const encoder = new TextEncoder();
                const keyData = encoder.encode(config.apiSecret);

                crypto.subtle.importKey(
                    'raw',
                    keyData,
                    { name: 'HMAC', hash: 'SHA-256' },
                    false,
                    ['sign']
                ).then(key => {
                    return crypto.subtle.sign(
                        'HMAC',
                        key,
                        encoder.encode(signatureOrigin)
                    );
                }).then(signature => {
                    // 将ArrayBuffer转换为Base64
                    const signatureArray = Array.from(new Uint8Array(signature));
                    const signatureBase64 = btoa(String.fromCharCode(...signatureArray));

                    // 构造authorization base64编码
                    const authorizationOrigin = `api_key="${config.apiKey}", algorithm="hmac-sha256", headers="host date request-line", signature="${signatureBase64}"`;
                    const authorization = btoa(authorizationOrigin);

                    // 编码日期
                    const encodedDate = encodeURIComponent(date);

                    // 构造最终URL
                    const finalUrl = `${config.apiUrl}?authorization=${authorization}&date=${encodedDate}&host=iat-api.xfyun.cn`;
                    resolve(finalUrl);
                }).catch(err => {
                    reject(`签名生成失败: ${err.message}`);
                });
            } catch (err) {
                reject(`加密API不可用: ${err.message}`);
            }
        });
    }

    // 初始化WebSocket连接
    async function initWebSocket() {
        try {
            const url = await getWebSocketUrl();
            websocket = new WebSocket(url);

            websocket.onopen = () => {
                console.log('WebSocket连接已建立');

                // 发送初始化消息 (增加静音检测时间)
                const initData = {
                    common: {
                        app_id: config.appId
                    },
                    business: {
                        language: 'zh_cn',
                        domain: 'iat',
                        accent: 'mandarin',
                        vad_eos: MAX_THINKING_TIME  // 使用最大思考时间
                    },
                    data: {
                        status: 0,
                        format: 'audio/L16;rate=16000',
                        encoding: 'raw'
                    }
                };

                websocket.send(JSON.stringify(initData));
            };

            websocket.onmessage = (event) => {
                const data = JSON.parse(event.data);
                console.log('收到消息:', data);

                if (data.code !== 0) {
                    console.error('API错误:', data);
                    if (statusEl) statusEl.textContent = `错误: ${data.message}`;
                    return;
                }

                const result = data.data && data.data.result;
                if (result) {
                    const str = result.ws.map(ws => {
                        return ws.cw.map(cw => cw.w).join('');
                    }).join('');

                    currentTranscript += str;
                    if (recognitionResult) recognitionResult.textContent = currentTranscript;

                    // 如果这是最终结果
                    if (data.data.status === 2) {
                        finalRecognitionResult = currentTranscript;
                        isFinalResultReceived = true;
                        if (recognitionResult) recognitionResult.textContent = finalRecognitionResult;
                        if (submitAnswerBtn) submitAnswerBtn.disabled = false;
                        currentTranscript = '';
                    }
                }
            };

            websocket.onerror = (error) => {
                console.error('WebSocket错误:', error);
                if (statusEl) statusEl.textContent = '连接错误';
                stopRecording();
            };

            websocket.onclose = () => {
                console.log('WebSocket连接关闭');
            };

            return true;
        } catch (error) {
            console.error('初始化WebSocket失败:', error);
            if (statusEl) statusEl.textContent = `错误: ${error}`;
            return false;
        }
    }

    // 开始录音
    async function startRecording() {
        console.log("尝试开始录音...");

        if (isRecording) {
            console.log("已经在录音中");
            return;
        }

        try {
            // 检查API配置 - 如果未配置则从后端获取
            if (!config.appId || !config.apiKey || !config.apiSecret) {
                const success = await fetchConfigFromBackend();
                if (!success) {
                    alert('无法获取API配置，请刷新页面重试');
                    return;
                }
            }

            // 重置识别结果状态
            finalRecognitionResult = "";
            isFinalResultReceived = false;
            isPaused = false; // 重置暂停状态
            if (recognitionResult) recognitionResult.textContent = "语音识别中...";
            if (recognitionResult) {
                recognitionResult.innerHTML = "请开始回答...<br><small>（您有最多10秒的思考时间）</small>";
            }

            // 创建音频上下文和可视化
            audioContext = new (window.AudioContext || window.webkitAudioContext)({
                sampleRate: 16000 // 设置采样率为16000Hz
            });

            // 创建可视化器
            analyser = audioContext.createAnalyser();
            analyser.fftSize = 256;
            createVisualizerBars();

            // 获取麦克风输入
            mediaStream = await navigator.mediaDevices.getUserMedia({
                audio: {
                    sampleRate: 16000, // 设置采样率
                    channelCount: 1,   // 单声道
                    echoCancellation: true,
                    noiseSuppression: true,
                    autoGainControl: false
                }
            });
            microphone = audioContext.createMediaStreamSource(mediaStream);

            // 创建处理节点
            audioProcessor = audioContext.createScriptProcessor(4096, 1, 1);

            // 连接节点
            microphone.connect(analyser);
            analyser.connect(audioProcessor);
            audioProcessor.connect(audioContext.destination);

            // 处理音频数据
            audioProcessor.onaudioprocess = (event) => {
                // 获取音频数据
                const inputData = event.inputBuffer.getChannelData(0);

                // 转换为Int16Array
                const int16Data = new Int16Array(inputData.length);
                for (let i = 0; i < inputData.length; i++) {
                    int16Data[i] = inputData[i] * 32767;
                }

                // 将数据添加到队列
                audioQueue.push(int16Data);
                audioSamplesQueue = [...audioSamplesQueue, ...inputData];

                // 更新可视化
                updateVisualization();
            };

            // 启动发送数据的定时器
            sendInterval = setInterval(sendAudioData, 40);

            // 初始化WebSocket
            const wsSuccess = await initWebSocket();
            if (!wsSuccess) {
                console.log("WebSocket初始化失败");
                stopRecording();
                return;
            }

            // 更新状态
            isRecording = true;
            console.log("录音已开始");
            if (statusEl) {
                statusEl.textContent = '录音中...';
                statusEl.classList.add('recording');
            }
            if (startRecordBtn) startRecordBtn.disabled = true;
            if (stopRecordBtn) stopRecordBtn.disabled = false;
            if (pauseRecordBtn) pauseRecordBtn.disabled = false;
            currentTranscript = '';
            if (recognitionResult) recognitionResult.textContent = '正在识别...';
            if (recordingIndicator) recordingIndicator.classList.remove('hidden');

            // 启动思考时间计时器
            startThinkingTimer();
        } catch (error) {
            console.error('开始录音失败:', error);
            if (statusEl) statusEl.textContent = `错误: ${error.message || error}`;
            stopRecording();
        }
    }

    // 停止录音 (添加结束帧)
    function stopRecording() {
        if (!isRecording) return;

        console.log("停止录音...");

        // 清除发送数据的定时器
        if (sendInterval) {
            clearInterval(sendInterval);
            sendInterval = null;
        }

        // 清除思考时间计时器
        clearInterval(timeRemainingInterval);
        if (timeRemainingEl) timeRemainingEl.textContent = '';

        // 关闭音频处理
        if (audioProcessor) {
            audioProcessor.disconnect();
            audioProcessor.onaudioprocess = null;
            audioProcessor = null;
        }

        if (microphone) {
            microphone.disconnect();
            microphone = null;
        }

        if (analyser) {
            analyser.disconnect();
            analyser = null;
        }

        if (audioContext) {
            audioContext.close().catch(console.error);
            audioContext = null;
        }

        // 关闭媒体流
        if (mediaStream) {
            mediaStream.getTracks().forEach(track => track.stop());
            mediaStream = null;
        }

        // 将当前识别结果作为最终结果
        finalRecognitionResult = currentTranscript;
        isFinalResultReceived = true;

        // 关闭WebSocket (添加结束帧)
        if (websocket) {
            if (websocket.readyState === WebSocket.OPEN) {
                // 发送结束帧
                const endData = {
                    data: {
                        status: 2,  // 结束标识
                        format: 'audio/L16;rate=16000',
                        encoding: 'raw',
                        audio: ''
                    }
                };
                websocket.send(JSON.stringify(endData));
            }
            websocket.close();
            websocket = null;
        }

        // 清空音频缓冲区
        audioQueue = [];
        audioSamplesQueue = [];

        // 更新状态
        isRecording = false;
        isPaused = false;
        if (statusEl) {
            statusEl.textContent = '已停止录音';
            statusEl.classList.remove('recording');
        }
        if (startRecordBtn) startRecordBtn.disabled = false;
        if (stopRecordBtn) stopRecordBtn.disabled = true;
        if (pauseRecordBtn) {
            pauseRecordBtn.disabled = true;
            pauseRecordBtn.innerHTML = '<i class="fas fa-pause"></i> 暂停录音';
            pauseRecordBtn.classList.remove('btn-success');
            pauseRecordBtn.classList.add('btn-warning');
        }
        if (recordingIndicator) recordingIndicator.classList.add('hidden');

        console.log('录音已停止');

        // 直接启用提交按钮
        if (submitAnswerBtn) {
            submitAnswerBtn.disabled = false;
        }

        // 显示当前识别结果
        if (recognitionResult) {
            recognitionResult.textContent = finalRecognitionResult || "无识别内容";
        }
    }

    // 绘制技能雷达图
    function renderRadarChart(scores) {
        const canvas = document.getElementById('skillRadarChart');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        if (radarChart) {
            radarChart.destroy();
        }

        radarChart = new Chart(ctx, {
            type: 'radar',
            data: {
                labels: ['专业知识', '问题解决', '沟通表达', '技术深度', '逻辑思维'],
                datasets: [{
                    label: '技能评估',
                    data: scores,
                    backgroundColor: 'rgba(67, 97, 238, 0.2)',
                    borderColor: 'rgba(67, 97, 238, 1)',
                    borderWidth: 2,
                    pointBackgroundColor: 'rgba(67, 97, 238, 1)',
                    pointBorderColor: '#fff',
                    pointHoverBackgroundColor: '#fff',
                    pointHoverBorderColor: 'rgba(67, 97, 238, 1)'
                }]
            },
            options: {
                scales: {
                    r: {
                        angleLines: {
                            display: true
                        },
                        suggestedMin: 0,
                        suggestedMax: 10,
                        ticks: {
                            stepSize: 2
                        }
                    }
                },
                plugins: {
                    legend: {
                        position: 'top',
                    },
                    title: {
                        display: true,
                        text: '技能维度评估',
                        font: {
                            size: 16
                        }
                    }
                }
            }
        });
    }

    // 调用后端API生成面试问题函数
        async function generateInterviewQuestion(career, difficulty) {
            try {
                    console.log(`生成面试问题 - 职业方向: ${career}, 难度级别: ${difficulty}`);

                    const response = await fetch(`${API_BASE_URL}/api/interview/start`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                            careerDirection: career,
                            difficultyLevel: parseInt(difficulty)
                        })
                    });

                    if (!response.ok) {
                        throw new Error(`HTTP错误: ${response.status}`);
                    }

                    const result = await response.json();
                    console.log("后端返回数据:", result);

                    if (response.ok && result.code === 200 && result.data) {
                        const questionData = result.data;
                        console.log("问题数据:", questionData);

                        return {
                            interviewId: questionData.interviewId,
                            sessionId: questionData.sessionId,
                            question: questionData.questionText
                        };
                    } else {
                        throw new Error(result.message || '生成问题失败');
                    }
                } catch (error) {
                    console.error('生成问题失败:', error);

                    return {
                        interviewId: "error-" + Date.now(),
                        sessionId: "error-" + Date.now(),
                        question: "问题生成失败: " + error.message
                    };
                }
        }

    // 开始面试
    startInterviewBtn.addEventListener('click', async function() {
        const career = document.getElementById('careerDirection').value;
        const difficulty = document.getElementById('difficultyLevel').value;

        if (!career) {
            alert('请选择职业方向');
            return;
        }

        console.log("开始面试 - 职业方向:", career, "难度级别:", difficulty);

        // 禁用按钮并显示加载状态
        if (startInterviewBtn) {
            startInterviewBtn.disabled = true;
            startInterviewBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 生成中...';
        }
        if (questionSection) questionSection.classList.remove('hidden');

        // 重置识别结果状态
        finalRecognitionResult = "";
        isFinalResultReceived = false;
        if (recognitionResult) recognitionResult.textContent = "您的回答将实时显示在这里...";

        try {
            // 调用后端API生成问题
            const questionData = await generateInterviewQuestion(career, difficulty);
            console.log("生成的问题:", questionData.question, "sessionId:", questionData.sessionId);

            // 保存sessionId和当前问题
            currentSessionId = questionData.sessionId;
            currentInterviewId = questionData.interviewId;  // 新增：保存数据库ID
            currentQuestion = questionData.question;

            if (questionContent) questionContent.textContent = currentQuestion;

            // 重置按钮状态
            if (startInterviewBtn) {
                startInterviewBtn.disabled = false;
                startInterviewBtn.innerHTML = '<i class="fas fa-redo"></i> 重新生成问题';
            }
        } catch (error) {
            console.error('生成问题失败:', error);
            // 显示错误信息
            if (questionContent) questionContent.textContent = "问题生成失败: " + error.message;
            // 重置按钮状态
            if (startInterviewBtn) {
                startInterviewBtn.disabled = false;
                startInterviewBtn.innerHTML = '<i class="fas fa-play-circle"></i> 开始面试';
            }
        }
    });

    // 开始录音
    if (startRecordBtn) {
        startRecordBtn.addEventListener('click', startRecording);
    }

    // 停止录音
    if (stopRecordBtn) {
        stopRecordBtn.addEventListener('click', stopRecording);
    }

    // ============ 新增：暂停/继续录音 ============
    if (pauseRecordBtn) {
        pauseRecordBtn.addEventListener('click', function() {
            if (!isRecording) return;

            if (isPaused) {
                resumeRecording();
            } else {
                pauseRecording();
            }
        });
    }

    // 提交回答
    if (submitAnswerBtn) {
        submitAnswerBtn.addEventListener('click', async function() {
            const question = currentQuestion || (questionContent ? questionContent.textContent : "");
            const answer = finalRecognitionResult || (recognitionResult ? recognitionResult.textContent : "");

            console.log("提交回答 - 问题:", question, "回答:", answer, "sessionId:", currentSessionId);

            if (!answer || answer === "您的回答将实时显示在这里..." ||
                answer === "语音识别中..." || answer === "正在获取最终结果...") {
                alert('请先完成录音回答');
                return;
            }

            if (!currentSessionId) {
                console.error("缺少 sessionId");
                alert('系统错误：缺少会话ID');
                return;
            }

            // 显示加载状态
            if (submitAnswerBtn) {
                submitAnswerBtn.disabled = true;
                submitAnswerBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 分析回答中...';
            }

            try {
                console.log("调用后端反馈接口...");
                const response = await fetch(`${API_BASE_URL}/api/interview/submit-answer`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({
                        interviewId: currentInterviewId,  // 使用interviewId而不是sessionId
                        answerText: answer,
                        userId: 'anonymous'
                    })
                });

                console.log("后端响应状态:", response.status);

                if (!response.ok) {
                    const errorText = await response.text();
                    console.error("后端返回错误:", errorText);
                    throw new Error(`请求失败: ${response.status} ${response.statusText}`);
                }

                const result = await response.json();
                if (result.code === 200 && result.data) {
                    const questionData = result.data;
                    currentInterviewId = questionData.interviewId;  // 保存interviewId
                    currentQuestion = questionData.questionText;

                    // 显示问题
                    if (questionContent) {
                        questionContent.textContent = currentQuestion;
                    }
                }
                console.log("后端返回数据:", result);

                // 检查响应结构
                if (result.code !== 200) {
                    console.error("后端业务错误:", result.message);
                    throw new Error(result.message || '反馈生成失败');
                }

                const feedbackData = result.data;
                console.log("反馈数据:", feedbackData);

                // 检查必要字段
                if (!feedbackData || !feedbackData.overallScore) {
                    console.error("无效的反馈数据:", feedbackData);
                    throw new Error('返回的反馈数据格式不正确');
                }

                // ============ 填充反馈页面数据 ============
                // 填充问题
                if (feedbackQuestion) feedbackQuestion.textContent = question;

                // 填充用户回答
                if (userAnswer) userAnswer.textContent = answer;

                // 填充总体反馈
                if (overallFeedback) {
                    overallFeedback.textContent = feedbackData.overallFeedback || "暂无总体评价";
                }

                // 填充改进建议
                if (improvementSuggestions) {
                    const suggestions = feedbackData.improvementSuggestions || [];
                    improvementSuggestions.innerHTML = suggestions.map(sugg =>
                        `<div class="suggestion-item"><i class="fas fa-lightbulb text-warning"></i> ${sugg}</div>`
                    ).join('');

                    if (suggestions.length === 0) {
                        improvementSuggestions.innerHTML = '<div class="suggestion-item"><i class="fas fa-info-circle"></i> 暂无改进建议</div>';
                    }
                }

                // 处理维度评价
                let technicalFeedbackText = "";
                let communicationFeedbackText = "";

                if (feedbackData.dimensions && feedbackData.dimensions.length > 0) {
                    // 查找技术深度维度
                    const technicalDim = feedbackData.dimensions.find(d =>
                        d.dimensionName && (
                            d.dimensionName.includes('技术') ||
                            d.dimensionName.includes('专业') ||
                            d.dimensionName.includes('Technical')
                        )
                    );

                    // 查找沟通表达维度
                    const communicationDim = feedbackData.dimensions.find(d =>
                        d.dimensionName && (
                            d.dimensionName.includes('沟通') ||
                            d.dimensionName.includes('表达') ||
                            d.dimensionName.includes('Communication')
                        )
                    );

                    if (technicalDim) {
                        technicalFeedbackText = `${technicalDim.dimensionName}: ${technicalDim.evaluation || "暂无评价"} (评分: ${technicalDim.score})`;
                    }

                    if (communicationDim) {
                        communicationFeedbackText = `${communicationDim.dimensionName}: ${communicationDim.evaluation || "暂无评价"} (评分: ${communicationDim.score})`;
                    }

                    // 准备雷达图数据（按固定顺序）
                    const radarLabels = ['专业知识', '问题解决', '沟通表达', '技术深度', '逻辑思维'];
                    const radarData = radarLabels.map(label => {
                        const dim = feedbackData.dimensions.find(d =>
                            d.dimensionName && d.dimensionName.includes(label.substring(0, 2))
                        );
                        // 如果有评分，转换为数字（假设评分是 0-10 或类似格式）
                        if (dim && dim.score) {
                            const scoreStr = dim.score.toString();
                            const match = scoreStr.match(/[\d.]+/);
                            return match ? parseFloat(match[0]) : 6;
                        }
                        return 6; // 默认值
                    });

                    // 渲染技能雷达图
                    renderRadarChart(radarData);
                }

                // 设置技术反馈和沟通反馈
                if (technicalFeedback) {
                    technicalFeedback.textContent = technicalFeedbackText || "技术维度暂无详细评价";
                }
                if (communicationFeedback) {
                    communicationFeedback.textContent = communicationFeedbackText || "沟通维度暂无详细评价";
                }

                // 设置分数
                const scoreMatch = feedbackData.overallScore.match(/[\d.]+/);
                const score = scoreMatch ? parseFloat(scoreMatch[0]) : 0;

                if (scoreValue) {
                    scoreValue.textContent = feedbackData.overallScore || "0/10";
                }

                if (scoreProgress) {
                    const percentage = Math.min(100, (score / 10) * 100);
                    scoreProgress.style.width = `${percentage}%`;
                    scoreProgress.style.transition = 'width 1s ease';

                    // 根据分数设置进度条颜色
                    if (score > 7) {
                        scoreProgress.style.backgroundColor = '#10b981'; // 绿色
                    } else if (score > 5) {
                        scoreProgress.style.backgroundColor = '#f59e0b'; // 橙色
                    } else {
                        scoreProgress.style.backgroundColor = '#ef4444'; // 红色
                    }
                }

                // 切换到反馈页面
                if (interviewPage) interviewPage.classList.add('hidden');
                if (feedbackPage) feedbackPage.classList.remove('hidden');

                console.log("反馈生成成功");

            } catch (error) {
                console.error('生成反馈失败:', error);

                // 显示错误信息，但仍然切换到反馈页面显示基本信息
                alert('生成反馈时出错: ' + error.message + '，将显示基础信息');

                // 即使出错也显示基础信息
                if (feedbackQuestion) feedbackQuestion.textContent = question;
                if (userAnswer) userAnswer.textContent = answer;
                if (overallFeedback) overallFeedback.textContent = "反馈生成过程中出现错误，请稍后重试";
                if (scoreValue) scoreValue.textContent = "0/10";
                if (scoreProgress) scoreProgress.style.width = "0%";

                // 切换到反馈页面
                if (interviewPage) interviewPage.classList.add('hidden');
                if (feedbackPage) feedbackPage.classList.remove('hidden');

            } finally {
                // 恢复按钮状态
                if (submitAnswerBtn) {
                    submitAnswerBtn.disabled = false;
                    submitAnswerBtn.innerHTML = '<i class="fas fa-paper-plane"></i> 提交回答';
                }
            }
        });
    }

    // 开始新的面试
    if (newInterviewBtn) {
        newInterviewBtn.addEventListener('click', function() {
            console.log("开始新的面试");
            // 重置面试页面
            const careerDirection = document.getElementById('careerDirection');
            const difficultyLevel = document.getElementById('difficultyLevel');

            if (careerDirection) careerDirection.value = '';
            if (difficultyLevel) difficultyLevel.value = '2';
            // 清除问题内容并隐藏问题区域
                    if (questionSection) {
                        questionSection.classList.add('hidden');
                        if (questionContent) questionContent.textContent = ""; // 清除问题内容
                    }
            if (recognitionResult) recognitionResult.textContent = "您的回答将实时显示在这里...";
            if (startInterviewBtn) {
                startInterviewBtn.disabled = false;
                startInterviewBtn.innerHTML = '<i class="fas fa-play-circle"></i> 开始面试';
            }
            if (startRecordBtn) startRecordBtn.disabled = false;
            if (stopRecordBtn) stopRecordBtn.disabled = true;
            if (pauseRecordBtn) pauseRecordBtn.disabled = true;
            if (submitAnswerBtn) {
                submitAnswerBtn.disabled = true;
                submitAnswerBtn.innerHTML = '<i class="fas fa-paper-plane"></i> 提交回答';
            }
            if (visualizer) visualizer.innerHTML = '';

            // 重置识别结果状态
            finalRecognitionResult = "";
            isFinalResultReceived = false;
            currentSessionId = null;
            currentQuestion = "";

            // 清除思考时间显示
            if (timeRemainingEl) timeRemainingEl.textContent = '';

            // 切换回面试页面
            if (feedbackPage) feedbackPage.classList.add('hidden');
            if (interviewPage) interviewPage.classList.remove('hidden');
        });
    }


// 下载报告（HTML格式）
if (downloadReportBtn) {
    downloadReportBtn.addEventListener('click', function() {
        console.log("下载HTML格式报告");

       // 获取报告内容
            const question = feedbackQuestion ? feedbackQuestion.textContent : "未记录问题";
            const answer = userAnswer ? userAnswer.textContent : "未记录回答";
            const feedback = overallFeedback ? overallFeedback.textContent : "无反馈";
            const technical = technicalFeedback ? technicalFeedback.textContent : "无技术反馈";
            const communication = communicationFeedback ? communicationFeedback.textContent : "无沟通反馈";
            const score = scoreValue ? scoreValue.textContent : "0/10";

            // 获取改进建议
            let suggestions = [];
            if (improvementSuggestions) {
                const suggestionItems = improvementSuggestions.querySelectorAll('.suggestion-item');
                suggestions = Array.from(suggestionItems).map(item =>
                    item.textContent.replace('lightbulb', '').trim()
                );
            }


        // 创建HTML格式报告
        const htmlContent = `
<!DOCTYPE html>
<html>
<head>
    <title>面试反馈报告</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; }
        .header { background: #4361ee; color: white; padding: 20px; text-align: center; }
        .section { margin: 20px 0; padding: 15px; border-left: 4px solid #4361ee; }
        .suggestions li { margin: 10px 0; }
        .footer { margin-top: 30px; text-align: center; color: #666; font-size: 0.9em; }
    </style>
</head>
<body>
    <div class="header">
        <h1>面试反馈报告</h1>
        <p>生成时间: ${new Date().toLocaleString()}</p>
    </div>

    <div class="section">
        <h2>问题</h2>
        <p>${question}</p>
    </div>

    <div class="section">
        <h2>回答</h2>
        <p>${answer}</p>
    </div>

    <div class="section">
        <h2>评估结果</h2>
        <p><strong>综合评分:</strong> ${score}</p>
        <p><strong>综合反馈:</strong> ${feedback}</p>
        <p><strong>技术反馈:</strong> ${technical}</p>
        <p><strong>沟通反馈:</strong> ${communication}</p>
    </div>

    <div class="section">
        <h2>改进建议</h2>
        <ul class="suggestions">
            ${suggestions.map(s => `<li>${s}</li>`).join('')}
        </ul>
    </div>

    <div class="footer">
        <p>© ${new Date().getFullYear()} 面试评估系统</p>
    </div>
</body>
</html>
        `;

        // 创建下载链接
        const blob = new Blob([htmlContent], { type: 'text/html' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `面试反馈报告_${new Date().toISOString().slice(0, 10)}.html`;
        document.body.appendChild(a);
        a.click();

        // 清理资源
        setTimeout(() => {
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        }, 100);

        alert('HTML格式面试报告已开始下载');
    });
}

    // 初始化应用
    createVisualizerBars();

    // 预加载配置
    fetchConfigFromBackend().catch(console.error);
});
