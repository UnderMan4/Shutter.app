import React, { useEffect, useState } from "react";
import { Card, Table } from "components/shared";
import { useTranslation } from "react-i18next";
import { useGetAccountChangeLogMutation } from "redux/service/usersManagementService";
import { GetOwnAccountChangeLogRequest } from "redux/types/api";
import { tableHeader } from "types/ComponentTypes";

interface Props {
   login: string;
}

export const AccountChangeLog: React.FC<Props> = ({ login }) => {
   const { t } = useTranslation();
   const [getAccountChangeLogMutation, getAccountChangeLogMutationState] =
      useGetAccountChangeLogMutation();
   const [params, setParams] = useState<GetOwnAccountChangeLogRequest>({
      pageNo: 1,
      recordsPerPage: 25,
      columnName: "id",
      order: "asc",
   });

   const [tableData, setTableData] = useState([]);
   useEffect(() => {
      getAccountChangeLogMutation({ params: params, pathParam: login });
   }, [params]);

   useEffect(() => {
      const list = getAccountChangeLogMutationState.data?.list?.map((item) => [
         item.id,
         new Date(item.changedAt).toUTCString(),
         item.changedBy,
         item.changeType,
      ]);
      list && setTableData(list);
   }, [getAccountChangeLogMutationState.data]);

   const [headers, setHeaders] = useState<tableHeader[]>([
      {
         id: "id",
         label: t("global.label.id"),
         sortable: true,
         sort: "asc",
      },
      {
         id: "changedAt",
         label: t("user_account_info_page.modification_date"),
         sortable: true,
         sort: null,
      },
      {
         id: "changedBy",
         label: t("user_account_info_page.modification_author"),
         sortable: true,
         sort: null,
      },
      {
         id: "changeType",
         label: t("user_account_info_page.modification_type"),
         sortable: true,
         sort: null,
      },
   ]);

   return (
      <Card className="account-change-log-wrapper">
         <Table
            headers={headers}
            data={tableData}
            setHeaders={setHeaders}
            allRecords={getAccountChangeLogMutationState.data?.allRecords}
            allPages={getAccountChangeLogMutationState.data?.allPages}
            pageNo={params.pageNo}
            setPageNo={(number: number) => setParams({ ...params, pageNo: number })}
            recordsPerPage={params.recordsPerPage}
            setRecordsPerPage={(number: number) =>
               setParams({ ...params, recordsPerPage: number })
            }
         />
      </Card>
   );
};
