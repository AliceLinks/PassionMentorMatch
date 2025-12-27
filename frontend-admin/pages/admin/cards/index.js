const api = require('../../../utils/request.js');
Page({
    data: {
        phone: '',
        cardTypeOptions: ['semester', 'annual', 'lifetime'],
        cardTypeIndex: 0,
        startDate: '',
        realNameImage: '',
        loading: false,
        // no persistent result text; use transient toast
    },
    onLoad() {
        // 设置默认日期为今天
        const today = new Date();
        const yyyy = today.getFullYear();
        const mm = (today.getMonth() + 1).toString().padStart(2, '0');
        const dd = today.getDate().toString().padStart(2, '0');
        this.setData({ startDate: `${yyyy}-${mm}-${dd}` });
        // 页面初始化
    },
    onPhoneInput(e) {
        this.setData({ phone: e.detail.value });
    },
    onCardTypeChange(e) {
        this.setData({ cardTypeIndex: e.detail.value });
    },
    onStartDateChange(e) {
        this.setData({ startDate: e.detail.value });
    },
    onRealNameImageInput(e) {
        this.setData({ realNameImage: e.detail.value });
    },

    onChooseImage() {
        wx.chooseMedia({
            count: 1,
            mediaType: ['image'],
            success: res => {
                try {
                    const file = res.tempFiles[0];
                    const filePath = file.tempFilePath;
                    const fileSize = file.size || 0;
                    // 限制文件大小为 5MB
                    const maxSize = 5 * 1024 * 1024;
                    if (fileSize > maxSize) {
                        wx.showToast({ title: '图片过大（>5MB），请选择更小的图片', icon: 'none' });
                        return;
                    }
                    const ext = (filePath.match(/\.[^.]+$/) || ['.jpg'])[0];
                    const cloudPath = 'realname/' + Date.now() + '-' + Math.floor(Math.random() * 10000) + ext;
                    // 确保 wx.cloud 可用
                    if (!wx.cloud || !wx.cloud.uploadFile) {
                        wx.showModal({ title: '错误', content: '云开发未初始化或当前环境不支持 wx.cloud.uploadFile', showCancel: false });
                        return;
                    }
                    // 显示上传 loading
                    try { wx.showLoading({ title: '上传中' }); } catch (e) { }
                    wx.cloud.uploadFile({
                        cloudPath,
                        filePath,
                        success: uploadRes => {
                            console.log('[upload] success', uploadRes);
                            // 优先尝试获取临时 URL
                            wx.cloud.getTempFileURL({
                                fileList: [uploadRes.fileID],
                                success: urlRes => {
                                    console.log('[getTempFileURL] success', urlRes);
                                    const url = urlRes.fileList && urlRes.fileList[0] && urlRes.fileList[0].tempFileURL;
                                    if (url) {
                                        this.setData({ realNameImage: url });
                                        wx.showToast({ title: '上传成功', icon: 'success' });
                                    } else {
                                        // 回退到 fileID
                                        this.setData({ realNameImage: uploadRes.fileID });
                                        wx.showToast({ title: '上传成功（使用fileID）', icon: 'success' });
                                    }
                                },
                                fail: (err) => {
                                    console.error('[getTempFileURL] fail', err);
                                    // 回退到 fileID，仍可将 fileID 发送给后端
                                    this.setData({ realNameImage: uploadRes.fileID });
                                    wx.showToast({ title: '上传成功（无法获取URL，已使用fileID）', icon: 'none' });
                                },
                                complete: () => {
                                    // no-op
                                }
                            });
                        },
                        fail: (err) => {
                            console.error('[upload] fail', err);
                            wx.showModal({ title: '上传失败', content: (err && err.errMsg) || '上传文件到云存储失败', showCancel: false });
                        },
                        complete: () => {
                            try { wx.hideLoading(); } catch (e) { }
                        }
                    });
                } catch (err) {
                    // 保证任何同步错误也会清理 loading 并给出提示
                    try { wx.hideLoading(); } catch (e) { }
                    console.error('chooseMedia success handler error:', err);
                    wx.showToast({ title: '选择或上传图片时发生错误', icon: 'none' });
                }
            },
            fail: () => {
                // 用户取消或选择失败，无需显示 loading，但确保状态一致
                try { wx.hideLoading(); } catch (e) { }
                wx.showToast({ title: '未选择图片', icon: 'none' });
            }
        });
    },
    onIssueCard() {
        const { phone, cardTypeOptions, cardTypeIndex, startDate, realNameImage } = this.data;
        if (!phone || !startDate) {
            wx.showToast({ title: '请填写完整信息', icon: 'none' });
            return;
        }
        this.setData({ loading: true, result: '' });
        api.post('/admin/cards', {
            phone,
            cardType: cardTypeOptions[cardTypeIndex],
            startDate,
            realNameImage
        }).then((data) => {
            // request.js 返回的是 res.data.data（后端 Result 的 data 字段）
            // 后端成功时会返回实际数据（例如 { action, card }）
            if (data) {
                wx.showToast({ title: '发放成功', icon: 'success' });
                // 清空表单并将起始日期重置为今天
                const today = new Date();
                const yyyy = today.getFullYear();
                const mm = (today.getMonth() + 1).toString().padStart(2, '0');
                const dd = today.getDate().toString().padStart(2, '0');
                this.setData({ phone: '', realNameImage: '', cardTypeIndex: 0, startDate: `${yyyy}-${mm}-${dd}` });
            } else {
                wx.showToast({ title: '发放成功', icon: 'success' });
            }
        }).catch((err) => {
            console.error('issueCard error', err);
            // request.js 已经会弹网络错误或后端 message，但这里也设置结果显示
            wx.showToast({ title: (err && err.message) ? err.message : '网络或服务错误', icon: 'none' });
        }).finally(() => {
            this.setData({ loading: false });
        });
    }
});
// 调试代码已移除，恢复为简洁实现