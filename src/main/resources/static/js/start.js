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
    const standardAnswer = document.getElementById('standardAnswer');
    // 新增：三个新反馈卡片的DOM元素引用
    const knowledgeFeedback = document.getElementById('knowledgeFeedback');
    const problemSolvingFeedback = document.getElementById('problemSolvingFeedback');
    const logicalThinkingFeedback = document.getElementById('logicalThinkingFeedback');

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
                        console.log("原始问题数据:", questionData);

                        // 提取问题文本（支持多种格式）
                        let questionText = questionData.questionText;

                        // 情况1：已经是纯文本问题
                        if (typeof questionText === 'string' &&
                            !questionText.includes('{') &&
                            !questionText.includes('"question":')) {
                            console.log("类型1: 纯文本问题");
                            return {
                                interviewId: questionData.interviewId,
                                sessionId: questionData.sessionId,
                                question: questionText
                            };
                        }

                        // 情况2：包含JSON对象
                        try {
                            // 清理Markdown标记
                            let cleanText = questionText
                                .replace(/```json/g, '')
                                .replace(/```/g, '')
                                .trim();

                            console.log("清理后文本:", cleanText);

                            // 尝试解析JSON
                            const parsed = JSON.parse(cleanText);

                            // 提取question字段
                            if (parsed.question) {
                                console.log("类型2: JSON对象中的question字段");
                                return {
                                    interviewId: questionData.interviewId,
                                    sessionId: questionData.sessionId,
                                    question: parsed.question
                                };
                            }

                            // 可能嵌套在data属性中
                            if (parsed.data && parsed.data.question) {
                                console.log("类型3: 嵌套在data属性中");
                                return {
                                    interviewId: questionData.interviewId,
                                    sessionId: questionData.sessionId,
                                    question: parsed.data.question
                                };
                            }

                            console.warn("解析成功但未找到question字段:", parsed);
                        } catch (e) {
                            console.error('JSON解析失败:', e);
                        }

                        // 情况3：尝试手动提取
                        if (typeof questionText === 'string') {
                            const questionMatch = questionText.match(/"question":\s*"([^"]+)"/);
                            if (questionMatch && questionMatch[1]) {
                                console.log("类型4: 正则提取question字段");
                                return {
                                    interviewId: questionData.interviewId,
                                    sessionId: questionData.sessionId,
                                    question: questionMatch[1]
                                };
                            }
                        }

                        // 最终回退：显示原始内容
                        console.warn("无法解析，使用原始文本");
                        return {
                            interviewId: questionData.interviewId,
                            sessionId: questionData.sessionId,
                            question: questionText
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

                // 填充参考答案
                if (standardAnswer) {
                    // 优先使用后端返回的referenceAnswer
                    if (feedbackData.referenceAnswer && feedbackData.referenceAnswer.trim() !== "") {
                        standardAnswer.textContent = feedbackData.referenceAnswer;
                        console.log("已显示参考答案，长度:", feedbackData.referenceAnswer.length);
                    } else {
                        // 如果后端没有返回，尝试从问题数据中获取
                        standardAnswer.textContent = "暂无参考答案";
                        console.log("后端未返回参考答案");
                    }
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


                // ============ 处理所有维度评价 ============
                let technicalFeedbackText = "";
                let communicationFeedbackText = "";
                let knowledgeFeedbackText = "";
                let problemSolvingFeedbackText = "";
                let logicalThinkingFeedbackText = "";

                if (feedbackData.dimensions && feedbackData.dimensions.length > 0) {
                    // 智能匹配维度函数
                    const matchDimension = (dimensions, targetKeywords) => {
                        // 首先尝试精确匹配
                        for (const dim of dimensions) {
                            if (!dim.dimensionName) continue;

                            const dimName = dim.dimensionName.trim();

                            // 精确匹配维度名称
                            if (targetKeywords.includes(dimName)) {
                                return dim;
                            }

                            // 关键词匹配
                            for (const keyword of targetKeywords) {
                                if (dimName.includes(keyword)) {
                                    return dim;
                                }
                            }
                        }
                        return null;
                    };

                    // 定义各维度的关键词
                    const dimensionKeywords = {
                        '专业知识': ['专业知识', '知识', '专业', '雏虎1'],
                        '技术深度': ['技术深度', '技术', '深度', '雏虎4'],
                        '问题解决': ['问题解决', '问题', '解决', '雏虎2'],
                        '逻辑思维': ['逻辑思维', '逻辑', '思维', '雏虎5'],
                        '沟通表达': ['沟通表达', '沟通', '表达', '雏虎3']
                    };

                    // 查找各维度
                    const knowledgeDim = matchDimension(feedbackData.dimensions, dimensionKeywords['专业知识']);
                    const technicalDim = matchDimension(feedbackData.dimensions, dimensionKeywords['技术深度']);
                    const problemSolvingDim = matchDimension(feedbackData.dimensions, dimensionKeywords['问题解决']);
                    const logicalThinkingDim = matchDimension(feedbackData.dimensions, dimensionKeywords['逻辑思维']);
                    const communicationDim = matchDimension(feedbackData.dimensions, dimensionKeywords['沟通表达']);

                    // 修改这里：去掉维度名称前缀
                    const createFeedbackText = (dim) => {
                        if (!dim) return "暂无详细评价";
                        // 只返回评价内容和评分，去掉维度名称
                        return `${dim.evaluation || "暂无评价"} (评分: ${dim.score || "无"})`;
                    };

                    technicalFeedbackText = createFeedbackText(technicalDim);
                    communicationFeedbackText = createFeedbackText(communicationDim);
                    knowledgeFeedbackText = createFeedbackText(knowledgeDim);
                    problemSolvingFeedbackText = createFeedbackText(problemSolvingDim);
                    logicalThinkingFeedbackText = createFeedbackText(logicalThinkingDim);

                    // 修改备选方案部分也要去掉维度名称
                    if (!knowledgeFeedbackText.includes("暂无") && feedbackData.dimensions.length >= 1) {
                        const dim = feedbackData.dimensions[0];
                        knowledgeFeedbackText = `${dim.evaluation || "暂无评价"} (评分: ${dim.score})`;
                    }

                    if (!problemSolvingFeedbackText.includes("暂无") && feedbackData.dimensions.length >= 2) {
                        const dim = feedbackData.dimensions[1];
                        problemSolvingFeedbackText = `${dim.evaluation || "暂无评价"} (评分: ${dim.score})`;
                    }

                    if (!logicalThinkingFeedbackText.includes("暂无") && feedbackData.dimensions.length >= 5) {
                        const dim = feedbackData.dimensions[4];
                        logicalThinkingFeedbackText = `${dim.evaluation || "暂无评价"} (评分: ${dim.score})`;
                    }

                    // 准备雷达图数据（按固定顺序）
                    const radarLabels = ['专业知识', '问题解决', '沟通表达', '技术深度', '逻辑思维'];
                    const radarData = radarLabels.map(label => {
                        let dim;
                        switch(label) {
                            case '专业知识': dim = knowledgeDim; break;
                            case '问题解决': dim = problemSolvingDim; break;
                            case '沟通表达': dim = communicationDim; break;
                            case '技术深度': dim = technicalDim; break;
                            case '逻辑思维': dim = logicalThinkingDim; break;
                            default: dim = null;
                        }

                        // 如果有评分，转换为数字
                        if (dim && dim.score) {
                            const scoreStr = dim.score.toString();
                            let scoreValue;
                            if (scoreStr.includes('/')) {
                                const match = scoreStr.match(/[\d.]+/);
                                scoreValue = match ? parseFloat(match[0]) : 6;
                            } else {
                                const num = parseFloat(scoreStr);
                                // 如果数字大于10，假设是乘以100的结果，除以100
                                scoreValue = num > 10 ? num / 100 : num;
                            }
                            return scoreValue;
                        }
                    });

                    // 渲染技能雷达图
                    renderRadarChart(radarData);
                }

                // 设置所有反馈卡片的内容
                if (technicalFeedback) {
                    technicalFeedback.textContent = technicalFeedbackText || "技术维度暂无详细评价";
                }
                if (communicationFeedback) {
                    communicationFeedback.textContent = communicationFeedbackText || "沟通维度暂无详细评价";
                }
                if (knowledgeFeedback) {
                    knowledgeFeedback.textContent = knowledgeFeedbackText || "专业知识维度暂无详细评价";
                }
                if (problemSolvingFeedback) {
                    problemSolvingFeedback.textContent = problemSolvingFeedbackText || "问题解决维度暂无详细评价";
                }
                if (logicalThinkingFeedback) {
                    logicalThinkingFeedback.textContent = logicalThinkingFeedbackText || "逻辑思维维度暂无详细评价";
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
        const knowledge = knowledgeFeedback ? knowledgeFeedback.textContent : "无专业知识反馈";
        const problemSolving = problemSolvingFeedback ? problemSolvingFeedback.textContent : "无问题解决反馈";
        const logicalThinking = logicalThinkingFeedback ? logicalThinkingFeedback.textContent : "无逻辑思维反馈";
        const score = scoreValue ? scoreValue.textContent : "0/10";
        const referenceAnswer = standardAnswer ? standardAnswer.textContent : "无参考答案";

        // 获取改进建议
        let suggestions = [];
        if (improvementSuggestions) {
            const suggestionItems = improvementSuggestions.querySelectorAll('.suggestion-item');
            suggestions = Array.from(suggestionItems).map(item => {
                // 移除图标和多余空格
                const text = item.textContent.replace(/lightbulb|info-circle/g, '').trim();
                // 移除括号和里面的内容（如评分部分）
                return text.replace(/\s*\(.*?\)/g, '').trim();
            }).filter(sugg => sugg.length > 0);
        }

        // 创建HTML格式报告
        const htmlContent = `
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>面试反馈报告</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Microsoft YaHei', Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            background: #f8f9fa;
            padding: 20px;
        }

        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.08);
            overflow: hidden;
        }

        .header {
            background: linear-gradient(135deg, #4361ee 0%, #3a56d4 100%);
            color: white;
            padding: 30px 40px;
            text-align: center;
        }

        .header h1 {
            font-size: 32px;
            margin-bottom: 10px;
            font-weight: 600;
        }

        .header p {
            font-size: 16px;
            opacity: 0.9;
        }

        .section {
            margin: 25px 30px;
            padding: 25px;
            background: #fff;
            border-radius: 10px;
            border-left: 5px solid #4361ee;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
        }

        .section h2 {
            color: #4361ee;
            font-size: 24px;
            margin-bottom: 20px;
            padding-bottom: 10px;
            border-bottom: 2px solid #f0f0f0;
            font-weight: 600;
        }

        .section h3 {
            color: #555;
            font-size: 18px;
            margin: 20px 0 10px 0;
            font-weight: 500;
        }

        .content-box {
            background: #f8f9fa;
            border: 1px solid #e9ecef;
            border-radius: 8px;
            padding: 20px;
            margin: 15px 0;
            line-height: 1.7;
            font-size: 16px;
        }

        .content-box.question {
            border-left: 4px solid #4361ee;
            background: #f0f4ff;
        }

        .content-box.answer {
            border-left: 4px solid #10b981;
            background: #f0fdf4;
        }

        .content-box.reference {
            border-left: 4px solid #f59e0b;
            background: #fffbeb;
        }

        .score-section {
            text-align: center;
            margin: 25px 0;
            padding: 25px;
            background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
            border-radius: 10px;
        }

        .score-value {
            font-size: 42px;
            font-weight: 700;
            color: #4361ee;
            margin: 15px 0;
        }

        .score-label {
            font-size: 18px;
            color: #666;
            margin-bottom: 20px;
        }

        .dimension-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 20px;
            margin: 20px 0;
        }

        .dimension-card {
            background: white;
            border: 1px solid #e0e0e0;
            border-radius: 10px;
            padding: 20px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.04);
            transition: transform 0.2s, box-shadow 0.2s;
        }

        .dimension-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 4px 12px rgba(0,0,0,0.08);
        }

        .dimension-title {
            font-weight: 600;
            color: #4361ee;
            margin-bottom: 12px;
            font-size: 18px;
            display: flex;
            align-items: center;
        }

        .dimension-title i {
            margin-right: 8px;
            font-size: 16px;
        }

        .dimension-content {
            color: #555;
            font-size: 15px;
            line-height: 1.6;
        }

        .suggestions-list {
            list-style: none;
            padding: 0;
        }

        .suggestions-list li {
            background: #f8f9fa;
            margin: 12px 0;
            padding: 15px;
            border-left: 4px solid #10b981;
            border-radius: 6px;
            display: flex;
            align-items: flex-start;
        }

        .suggestions-list li i {
            color: #10b981;
            margin-right: 12px;
            margin-top: 3px;
            font-size: 16px;
        }

        .footer {
            margin-top: 40px;
            text-align: center;
            color: #666;
            font-size: 14px;
            padding: 20px;
            border-top: 1px solid #eee;
            background: #f8f9fa;
        }

        .highlight {
            color: #4361ee;
            font-weight: 600;
        }

        .badge {
            display: inline-block;
            padding: 4px 12px;
            background: #4361ee;
            color: white;
            border-radius: 20px;
            font-size: 14px;
            margin-left: 10px;
        }

        @media print {
            body { background: white; padding: 0; }
            .container { box-shadow: none; }
            .dimension-card { break-inside: avoid; }
        }
    </style>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>智能面试反馈报告</h1>
            <p>生成时间: ${new Date().toLocaleString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' })}</p>
        </div>

        <div class="section">
            <h2>面试问题</h2>
            <div class="content-box question">
                <i class="fas fa-question-circle" style="color: #4361ee; margin-right: 8px;"></i>
                ${question}
            </div>
        </div>

        <div class="section">
            <h2>候选人的回答</h2>
            <div class="content-box answer">
                <i class="fas fa-microphone" style="color: #10b981; margin-right: 8px;"></i>
                ${answer}
            </div>
        </div>

        <div class="section">
            <h2>参考答案</h2>
            <div class="content-box reference">
                <i class="fas fa-lightbulb" style="color: #f59e0b; margin-right: 8px;"></i>
                ${referenceAnswer}
            </div>
        </div>

        <div class="section">
            <h2>综合评估</h2>
            <div class="score-section">
                <div class="score-label">综合评分</div>
                <div class="score-value">${score}</div>
                <div class="content-box" style="margin-top: 20px;">
                    <h3 style="margin-top: 0; color: #4361ee;">总体评价</h3>
                    <p>${feedback}</p>
                </div>
            </div>
        </div>

        <div class="section">
            <h2>维度详细评估</h2>
            <div class="dimension-grid">
                <div class="dimension-card">
                    <div class="dimension-title">
                        <i class="fas fa-graduation-cap"></i> 专业知识
                    </div>
                    <div class="dimension-content">${knowledge}</div>
                </div>
                <div class="dimension-card">
                    <div class="dimension-title">
                        <i class="fas fa-tools"></i> 问题解决
                    </div>
                    <div class="dimension-content">${problemSolving}</div>
                </div>
                <div class="dimension-card">
                    <div class="dimension-title">
                        <i class="fas fa-comments"></i> 沟通表达
                    </div>
                    <div class="dimension-content">${communication}</div>
                </div>
                <div class="dimension-card">
                    <div class="dimension-title">
                        <i class="fas fa-code"></i> 技术深度
                    </div>
                    <div class="dimension-content">${technical}</div>
                </div>
                <div class="dimension-card">
                    <div class="dimension-title">
                        <i class="fas fa-brain"></i> 逻辑思维
                    </div>
                    <div class="dimension-content">${logicalThinking}</div>
                </div>
            </div>
        </div>

        <div class="section">
            <h2>改进建议</h2>
            ${suggestions.length > 0 ? `
                <ul class="suggestions-list">
                    ${suggestions.map((sugg, index) => `
                        <li>
                            <i class="fas fa-lightbulb"></i>
                            <span>${sugg}</span>
                        </li>
                    `).join('')}
                </ul>
            ` : '<div class="content-box" style="text-align: center; color: #666;"><i class="fas fa-info-circle"></i> 暂无改进建议</div>'}
        </div>

        <div class="footer">
            <p>© ${new Date().getFullYear()} 智能面试系统 - 专业面试评估与反馈</p>
            <p style="margin-top: 5px; font-size: 13px; color: #888;">本报告由AI智能生成，仅供参考学习</p>
        </div>
    </div>
</body>
</html>
        `;

        // 创建下载链接
        const blob = new Blob([htmlContent], { type: 'text/html;charset=utf-8' });
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