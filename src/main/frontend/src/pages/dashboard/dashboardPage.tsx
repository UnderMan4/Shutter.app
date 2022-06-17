import React, { useState } from "react";
import styles from "./dashboardPage.module.scss";
import { Button, ExpandableCard } from "components/shared";
import { useAppDispatch, useAppSelector } from "redux/hooks";
import { logout } from "redux/slices/authSlice";
import { Calendar } from "components/shared/calendar";

export const DashboardPage = () => {
   const dispatch = useAppDispatch();
   const { token } = useAppSelector((state) => state.auth);
   const [isOpen, setIsOpen] = useState(false);

   return (
      <section className={styles.dashboard_page_wrapper}>
         {token && (
            <Button
               onClick={() => {
                  dispatch(logout());
               }}
            >
               Logout
            </Button>
         )}
         <ExpandableCard isOpen={isOpen} setIsOpen={(open) => setIsOpen(open)}>
            <Calendar />
         </ExpandableCard>
      </section>
   );
};
