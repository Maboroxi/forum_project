import {get} from "@/net";

export const apiEmailRecordList = (success) =>
    get('/api/admin/email/list', success)
