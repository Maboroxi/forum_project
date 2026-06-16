const keyOf = userId => `topic_drafts_${userId}`

function readDrafts(userId) {
    if(!userId || userId < 0) return []
    const raw = localStorage.getItem(keyOf(userId))
    if(!raw) return []
    try {
        const drafts = JSON.parse(raw)
        return Array.isArray(drafts) ? drafts : []
    } catch (e) {
        console.warn('读取帖子草稿失败', e)
        return []
    }
}

function writeDrafts(userId, drafts) {
    localStorage.setItem(keyOf(userId), JSON.stringify(drafts))
}

function createDraftId() {
    return `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function normalizeContent(content) {
    if(!content) return null
    if(typeof content === 'string') {
        try {
            return JSON.parse(content)
        } catch {
            return null
        }
    }
    return JSON.parse(JSON.stringify(content))
}

function draftText(content) {
    const value = normalizeContent(content)
    if(!value?.ops) return ''
    return value.ops
        .map(op => typeof op.insert === 'string' ? op.insert : '')
        .join('')
        .replace(/\s/g, '')
}

export function listTopicDrafts(userId) {
    return readDrafts(userId).sort((a, b) => b.updateTime - a.updateTime)
}

export function getTopicDraft(userId, draftId) {
    return readDrafts(userId).find(item => item.id === draftId) || null
}

export function saveTopicDraft(userId, draft) {
    const now = Date.now()
    const drafts = readDrafts(userId)
    const id = draft.id || createDraftId()
    const saved = {
        id,
        title: draft.title || '',
        type: draft.type || null,
        content: normalizeContent(draft.content),
        createTime: draft.createTime || now,
        updateTime: now
    }
    const index = drafts.findIndex(item => item.id === id)
    if(index >= 0) {
        drafts.splice(index, 1, saved)
    } else {
        drafts.push(saved)
    }
    writeDrafts(userId, drafts)
    return saved
}

export function deleteTopicDraft(userId, draftId) {
    writeDrafts(userId, readDrafts(userId).filter(item => item.id !== draftId))
}

export function clearTopicDrafts(userId) {
    writeDrafts(userId, [])
}

export function topicDraftSummary(content, maxLength = 80) {
    const text = draftText(content)
    return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text
}
