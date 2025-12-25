<script setup>
import {RefreshRight, User} from "@element-plus/icons-vue";
import {apiEmailRecordList} from "@/net/api/email";
import {ref} from "vue";

const list = ref([])

apiEmailRecordList(data => {
    list.value = data
})
</script>

<template>
    <div class="email-admin">
        <div class="title">
            <el-icon><User/></el-icon>
            论坛邮件列表
        </div>
        <div class="desc">
            在这里管理论坛的所有发送的邮件，并操作重发。
        </div>
        <el-table :data="list" height="400">
            <el-table-column prop="id" label="ID" width="100" align="center"/>
            <el-table-column prop="email" label="收件人" width="200" align="center" show-overflow-tooltip/>
            <el-table-column prop="status" label="发送状态" width="100" align="center">
                <template #default="{ row }">
                    <el-tag v-if="row.status === 0" type="info">发送中</el-tag>
                    <el-tag v-if="row.status === 1" type="success">已发送</el-tag>
                    <el-tag v-if="row.status === 2" type="danger">失败</el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="title" label="邮件主题" width="200" align="center" show-overflow-tooltip/>
            <el-table-column prop="content" label="邮件内容" width="300" align="center" show-overflow-tooltip/>
            <el-table-column label="发送时间" width="200">
                <template #default="{ row }">
                    {{ new Date(row.time).toLocaleString() }}
                </template>
            </el-table-column>
            <el-table-column width="120" label="操作" fixed="right">
                <template #default="{ row }">
                    <el-button size="small" type="primary" :icon="RefreshRight"
                               :disabled="row.status !== 2">重新发送</el-button>
                </template>
            </el-table-column>
        </el-table>
    </div>
</template>

<style scoped>
.email-admin {
    .title {
        font-weight: bold;
    }

    .desc {
        color: #bababa;
        font-size: 13px;
        margin-bottom: 20px;
    }
}
</style>
